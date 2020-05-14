# [IndexedDISI（二）](https://www.amazingkoala.com.cn/Lucene/gongjulei/)（Lucene 8.4.0）

&emsp;&emsp;在文章[IndexedDISI（一）](https://www.amazingkoala.com.cn/Lucene/gongjulei/2020/0511/140.html)（阅读本文中之前，需要该前置文章）中我们介绍了在Lucene7.5.0中IndexedDISI的实现原理， 本文基于Lucene 8.4.0，将介绍优化后的IndexedDISI，即使用查找表（lookup table）提高了查询性能。

&emsp;&emsp;我们先根据源码中的注释看下优化的目的与方式，也可以直接查看[IndexedDISI.java](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/codecs/lucene80/IndexedDISI.java)文件中的Javadoc：

```text
To avoid O(n) lookup time complexity, with n being the number of documents, two lookup tables are used: A lookup table for block offset and index, and a rank structure for DENSE block index lookups.
```

&emsp;&emsp;上述大意是，查找的时间复杂度为O(n)，其中n是文档号的数量，优化方式为通过两个查找表来提高查询性能：

- 第一个查找表使用offset跟index**实现block之间的跳转**，在源码中，使用int类型数组来存储offset跟index的信息，该数组的变量名为jumps
- 第二个查找表使用rank结构（structure）**实现在block的稠密度为DENSE中的word之间的跳转**，数组的变量名为rank

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/1.png">

## 第一个查找表jumps

&emsp;&emsp;我们先看实现block之间跳转的查找表jumps数组，在jumps数组中，index跟offset作为一对（pair）来描述一个跳转信息：

- offset：该值描述了block在字节流的起始读取位置，例如在生成[索引文件.dvd&&.dim](https://www.amazingkoala.com.cn/Lucene/DocValues/)中使用IndexedDISI存储文档号时，offset就描述了block在索引文件.dvd中的起始读取位置
- index：block中第一个文档号的段内编号（见文章[IndexedDISI（一）](https://www.amazingkoala.com.cn/Lucene/gongjulei/2020/0511/140.html)）

### 生成jumps

&emsp;&emsp;我们通过一个例子来理解第一个查找表，如果我们以下的文档号集合：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/2.png">

&emsp;&emsp;在文章[IndexedDISI（一）](https://www.amazingkoala.com.cn/Lucene/gongjulei/2020/0511/140.html)我们说到，每个block用来描述**最多**2^16个文档号信息，例如第一个block中描述的文档号取值范围为[0, 2^16 - 1]，第二个block描述的文档号取值范围为[2^16, 2^17 - 1]，第三个block描述的文档号取值范围为[2^17, 2^18 - 1]，故图1中的文档号集合将会由3个block来存储如下所示：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/3.png">

&emsp;对于图3的情况，生成的jumps数组如下所示：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/4.png">

&emsp;&emsp;从图4可以看出，index描述的的确是block中第一个文档号的段内编号，例如在第三个block中，index的值为5003，意味着这个block中存储的第一个文档号的段内编是5003，至于为什么要存储index，我们将在系列文章[索引文件的生成（十五）之dvm&&dvd](https://www.amazingkoala.com.cn/Lucene/Index/2020/0507/139.html)中作出解释。

### 读取jumps

&emsp;&emsp;同样通过一个例子来介绍如果通过jumps实现block之间的跳转。如果我们有一个待查询的文档号为131082，由于每个block中描述的文档号的最多为2^16个，那么通过下面的公式获得结果，我们称之为inRangeBlockIndex（源码中的变量名），可以计算出文档号131082应该由哪个block来描述，在代码中：

```java
130182 >>> 16 = 2
```

&emsp;&emsp;由于jumps[ ]数组被存储在字节流中，所以继续根据根据下面的公式就可以获得inRangeBlockIndex为2对应的index跟offset的值：

```java
final int index = jumpTable.readInt(inRangeBlockIndex*Integer.BYTES*2);
final int offset = jumpTable.readInt(inRangeBlockIndex*Integer.BYTES*2+Integer.BYTES);
```

&emsp;&emsp;上述公式中Integer.BYTES的值为4，jumpTable为存储jumps[ ]数组的字节流，故我们以jumps[ ]数组的第一个数组元素作为起始位置，偏移值为inRangeBlockIndex\*Integer.BYTES\*2 + Integer.BYTES的位置就可以读取offset，即文档号131082用第二个block来描述，至于在不在这个block中，需要进一步进行判断，下文中会展开介绍：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/5.png">

&emsp;&emsp;在文章开头我们就说道，jumps数组是int类型，故4个数组元素占用16个字节。

## 第二个查找表rank

&emsp;&emsp;在一个block内部，我们根据block的稠密度使用不同的数据类型存储，只有稠密度为DENSE时，会使用第二个查找表rank数组来实现word之间的跳转：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/6.png">

### 生成rank

&emsp;&emsp;生成rank数组前，需要指定denseRankPower，该值描述的是block中每n个文档号就生成一个rank，对应关系如下：

表1：

| denseRankPower | 文档数量 |
| :------------: | :------: |
|       7        |   128    |
|       8        |   256    |
|       9        |   512    |
|       10       |   1024   |
|       11       |   2048   |
|       12       |   4096   |
|       13       |   8192   |
|       14       |  16384   |
|       15       |  32768   |

&emsp;&emsp;表一中，如果denseRankPower为7，那么block中每128个文档号就生成一个rank，由于一个word能最多表示64个文档号，意味着每2个word就生成一个rank，即图6中描述的那样。

&emsp;&emsp;在图6中，每处理两个word，就将这两个word中包含的文档数量，即bitCount写入到rank数组中，**注意的是bitCount是一个累加值**，在介绍完下面的例子后会发现，rank数组中的bitCount用来表示的是一个rank中第一个文档号的段内编号，例子中denseRankPower为7：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/7.png">

&emsp;&emsp;假设图6中的第一个block中存储了上述的文档号，其中图7中`文档号的数量`说明这个block的稠密度为DENSE（见文章p;在文章[IndexedDISI（一）](https://www.amazingkoala.com.cn/Lucene/gongjulei/2020/0511/140.html)），那么上述文档号在使用word存储后如下所示：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/8.png">

&emsp;&emsp;由于denseRankPower为7，意味着每两个word就要生成一个rank，并且两个word的bitCount和值记录到rank数组中：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（二）/9.png">

&emsp;&emsp;图9中，每个word的bitCount描述的是它存储的文档号数量，例如第一个word中存储了文档号1、56、61共三个文档号，故bitCount的值为3，由于两个word生成一个rank，所以两个word的bitCount的和值写入到rank数组的下标值为0的位置，即<font color=Red>红框</font>圈出的两个bitCount的和值，另外在继续处理了第三个、第四个共两个word后，我们此时需要生成第二个rank，不仅仅要获得这，两个word中的bitCount和值，还要累加上第一个跟第二个word的和值，即3 + 2 + 3 + 2 = 10，写入到rank数组的下标值为1的位置。

&emsp;&emsp;图6中的HEB、LEB是 高八位 high eight bit、 lower eight bit的缩写，由于一个block中最多有65536个文档号，意味着bitCount的值最大值为65535，需要16个bit字节才能表示，加上rank数组是字节数组，所以需要两个数组元素即2个字节才能表示bitCount的最大值，故第一个字节用来表示高8位，第二个字节用来表示低8位。

### 读取rank

&emsp;&emsp;在读取阶段，使用rank数组进行跳转的条件如下所示：

```java
denseRankPower != -1 && targetWordIndex - wordIndex >= (1 << (denseRankPower-6) )
```

&emsp;&emsp;上述条件中，wordIndex是上一个文档号的段内编号，targetWordIndex是当前查询文档号的段内编号，可见只有前后两次查询的段内编号超过denseRankPower对应的文档数量才会进行跳转，理由很简单，读取阶段只会读取出denseRankPower对应的文档号信息到内存，如果满足上述条件，我们需要从磁盘（缓冲）中读取新的words，具体的读取过程由于跟读取jumps数组类似就不赘述了。

## 结语

&emsp;&emsp; 在Lucene8.0.0之后，通过两个查找表使得在时间复杂度为O(n)的基础上提高了查询性能，对于表一中denseRankPower，源码中作者建议的取值范围为 [8, 12]，更多关于two lookup table的设计思想见这个issue：https://issues.apache.org/jira/browse/LUCENE-8585?jql=text%20~%20%22indexedDISI%22 。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/utils/IndexedDISI/IndexedDISI（二）/IndexedDISI（二）.zip)下载附件