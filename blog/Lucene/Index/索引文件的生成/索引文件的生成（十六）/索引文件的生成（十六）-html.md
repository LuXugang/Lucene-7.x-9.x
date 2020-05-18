# [索引文件的生成（十六）](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.4.0）

&emsp;&emsp;在文章[索引文件的生成（十五）之dvm&&dvd](https://www.amazingkoala.com.cn/Lucene/Index/2020/0507/139.html)中，我们介绍了在索引（index）阶段收集文档的NumericDocValues信息的内容，随后在flush阶段，会根据收集到的信息生成索引文件.dvd&&.dvm。如果已经阅读了文章[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)，那么能快的理解本文的内容，注意的是那篇文章中，是基于Lucene 7.5.0，下文中的内容基于Lucene 8.4.0，不一致（优化）的地方会特别说明。

## 生成索引文件.dvd、.dvm之NumericDocValues的流程图

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/1.png">

### 准备工作

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/2.png">

&emsp;&emsp;根据在索引阶段收集的文档的NumericDocValuses信息（见文章[索引文件的生成（十五）之dvm&&dvd](https://www.amazingkoala.com.cn/Lucene/Index/2020/0507/139.html)），在当前流程点需要执行一些准备工作，即计算出下面的信息：

- gcd（greatest common divisor）
- minMax和blockMinMax
- uniqueValues
- numDocsWithValue

&emsp;&emsp;在流程点`准备工作`中，通过**遍历每一个包含NumericDocValues信息的文档**来完成上述信息的计算。

#### gcd（greatest common divisor）

&emsp;&emsp;gcd即最大公约数，获得所有NumericDocValues的域值的最大公约数，根据数学知识可知，最大公约数的最小为1。

&emsp;&emsp;**为什么要计算gcd：**

&emsp;&emsp;在文章[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)中已经作出了解释，本文中再详细的介绍一次，其实目的很简单：降低存储开销。

&emsp;&emsp;例如我们有以下的域值集合：

```text
{150、140、135}
```

&emsp;&emsp;上述三个域值的最大公约数为5，即gcd的值为5，按照下面的公式我们计算出编码后的域值：

```java
(v - min) / gcd
```

&emsp;&emsp;上述公式中，v为编码前的域值，min为域值集合中的最小值，在本例子中，min的值为135，编码后的域值集合如下所示：

```text
{3, 1, 0}
```

&emsp;&emsp;可见编码后的域值集合中，最大值为3，当使用`固定位数按位存储`（见文章[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)）时，只需要6个bit存储即可，在读取阶段，根据min、gcd的值就可以获得编码前的域值集合。

#### minMax、blockMinMax

&emsp;&emsp;minMax跟blockMinMax在源码中都是[类MinMaxTracker](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/codecs/lucene80/Lucene80DocValuesConsumer.java)的对象，都是用来收集下面的信息：

- min：域值集合中的最小值
- max：域值集合中的最大值
- numValues：域值集合中的元素数量
- spaceInBits：存储域值占用的bit数量
  - spaceInBits的计算方式：(bitCount(max - min)) * num，其中num指的是域值的数量，bitCount( )方法描述的是域值占用的有效bit数量，例如bitCount(3) = 2，数值3的二进制为0b000000<font color=Red>11</font>，有效的bit数量为2个

&emsp;&emsp;两个信息的区别在于，minMax收集的是域值集合中全量数据下的min、max、spaceInBits，而blockMinMax则是将域值集合分为N个block，每处理**4096个域值**就作为一个block，每个block单独收集min、max、spaceInBits。

&emsp;&emsp;**为什么要计算minMax、blockMinMax**

&emsp;&emsp;为了判断出使用单个block和多个block存储域值时，哪一种方式有更低的存储开销。

&emsp;&emsp;例如我们有以下域值集合，为了便于描述，计算blockMinMax时，每处理**3个域值**就作为一个block。

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/3.png">

&emsp;&emsp;图2中，对于minMax，域值集合的全量数据下的min、max分别为1、18，并且有9个，那么spaceInBits = (bitCount(18 - 1)) * 9，数值17的二进制为0b000<font color=Red>10001</font>，有效的bit数量为5个，故spaceInBits的值为5\*9 = 45。

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/4.png">

&emsp;&emsp;图3中，对于blockMinMax，域值集合被分为3个block，每个block的spaceInBits的和值作为blockMinMax的spaceInBits，故spaceInBits = (bitCount(2 -1)) * 3 + (bitCount(18 -16)) * 3 + (bitCount(8 -5)) * 3 = 3 + 6 + 9 = 18。

&emsp;&emsp;源码中，如果blockMinMax和minMax的spaceInBits满足下面的条件，那么就使用多个block存储域值：

```java
(double) blockMinMax.spaceInBits / minMax.spaceInBits <= 0.9
```

&emsp;&emsp;显而易见，上述的例子将会使用多个block存储域值。当然了，当前的流程点是`准备工作`，仅仅判断出存储域值的方式，其具体的存储域值逻辑将在后面的流程中执行。

#### uniqueValues

&emsp;&emsp;uniqueValues是一个Set\<Long\>对象，用来统计域值的种类，uniqueValues将在后续的流程点`是否使用域值映射存储`作为判断的依据，在本文中不展开介绍，我们只需要知道收集域值种类的时机点即可，例如图3中的域值集合，在执行完流程点`准备工作`后，uniqueValues中的内容如下所示：

```text
{1, 2, 3, 5, 8, 16, 17, 18}
```

#### numDocsWithValue

&emsp;&emsp;numDocsWithValue用来描述包含当前域名的文档数量，在后面的流程中它将作为一个判断条件，用于选择使用哪种数据结构来存储包含当前域的文档号集合。

### 写入文档号信息

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/5.png">

&emsp;&emsp;文档号信息包含两个信息：

- 文档号的值信息：文档号的值将被写入到索引文件dvd中
- 文档号的索引信息：文档号索引信息将被写入到索引文件.dvm中，在读取阶段根据文档号的索引信息来读取在索引文件.dvd中的文档号的值区间，下文中会详细说明

&emsp;&emsp;另外根据在流程点`准备工作`中计算出的numDocsWithValue，对应有三种数据结构存储文档号索引信息。

#### 0 < numDocsWithValue < maxDoc

&emsp;&emsp;maxDoc的值为段中的文档数量，在下文中会介绍该值，如果numDocsWithValue的值在区间(0, maxDoc)，文档索引信息将会被存储到索引文件.dvm中的DocIdIndex字段，文档号的值信息将被存储到索引文件.dvd中的DocIdData字段：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/6.png">

&emsp;&emsp; 图6中，文档号的索引信息包含offset跟length，它们描述了文档号的值信息在索引文件.dvd中的值区间，其他字段的解释见文章[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)，我们再看Lucene 8.4.0中的数据结构：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/7.png">

&emsp;&emsp; 在Lucene 8.4.0版本中，DocIdIndex字段中多了jumpTableEntryCount跟denseRankPower两个信息，在读取阶段，通过这两个信息能获得查找表（lookup table）的信息，jumpTableEntryCount跟denseRankPower以及查找表的概念在系列文章[IndexedDISI](https://www.amazingkoala.com.cn/Lucene/gongjulei/2020/0511/140.html)已经介绍，这里不赘述。

#### numDocsWithValue == 0

&emsp;&emsp;如果numDocsWithValue == 0，那么将固定的信息写入到索引文件.dvm中，固定信息在索引文件.dvm中的位置如下所示：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/8.png">

&emsp;&emsp;图8中，offset跟length被置为固定信息，在读取阶段，当读取到offset的值为-2时，就知道numDocsWithValue == 0，下面给出Lucene 8.4.0的索引文件.dvm，由于numDocsWithValue == 0，文档号的值信息不用写入到索引文件.dvd中：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/9.png">

&emsp;&emsp;同样的查找表的信息用固定信息填充。

#### numDocsWithValue == maxDoc

&emsp;&emsp;如果numDocsWithValue == maxDoc，说明正在执行flush的段中的每篇文档都包含当前域的信息，意味着我们也不用存储文档号的值信息，因为在读取阶段，文档号的值就是 [0, maxDoc]区间内的所有值，故同样只要将固定的信息写入到索引文件.dvm中：

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/10.png">

&emsp;&emsp;在读取阶段，当读取到offset的值为-1时，就知道numDocsWithValue == maxDoc，同样地给出Lucene 8.4.0的数据结构：

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/11.png">

&emsp;&emsp;在写入了文档号信息之后，将域值数量写入到索引文件.dvm中，域值数量在上文中的minMax中numValues获得，如下所示：

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十六）/12.png">

## 结语

&emsp;&emsp;基于篇幅，剩余的内容将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的生成/索引文件的生成（十六）/索引文件的生成（十六）.zip)下载附件


