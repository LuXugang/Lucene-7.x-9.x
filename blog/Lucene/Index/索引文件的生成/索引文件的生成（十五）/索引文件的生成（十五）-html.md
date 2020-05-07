# [索引文件的生成（十五）](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.4.0）

&emsp;&emsp;在前面的文章中，我们介绍了在Lucene7.5.0中[索引文件.dvd&&.dvm](https://www.amazingkoala.com.cn/Lucene/DocValues/)的数据结构，从本篇文章开始介绍其生成索引文件.dvd&&.dvm的内容，注意的是，由于是基于Lucene8.4.0来描述其生成过程，故如果出现跟Lucene7.5.0中不一致的地方会另外指出，索引文件.dvd&&.dvm中的包含了下面几种类型：

- BinaryDocValues
- NumericDocValues
- SortedDocValues
- SortedNumericDocValues
- SortedSetDocValues

&emsp;&emsp;本篇文章从[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)开始介绍，建议先阅读下文章[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)，简单的了解NumericDocValues类型的DocValues的数据结构。

&emsp;&emsp;在文章[索引文件的生成（一）之doc&&pay&&pos](https://www.amazingkoala.com.cn/Lucene/Index/2019/1226/121.html)中，简单的介绍了生成[索引文件.dvd&&.dvm](https://www.amazingkoala.com.cn/Lucene/DocValues/)的时机点，为了能更好的理解其生成过程，会首先介绍下在生成索引文件之前，Lucene是如何收集每篇文档的NumericDocValues信息。

## 收集文档的NumericDocValues信息

&emsp;&emsp;在源码中，通过[NumericDocValuesWriter](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/index/NumericDocValuesWriter.java)对象来实现文档的NumericDocValues信息的收集，并且具有相同域名的NumericDocValues信息使用同一个NumericDocValuesWriter对象来收集，例如下图中添加三篇文档：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/1.png">

&emsp;&emsp;在使用NumericDocValuesField来生成NumericDocValues信息时要注意，在一篇文档中，仅能添加一条相同域名的NumericDocValuesField，但是能添加多条不同域名的NumericDocValuesField，如图1中，在文档0中添加了域名分别为"age"、"level"的NumericDocValuesField，如果我们在一篇文档中添加两个或以上相同域名的NumericDocValuesField，那么会抛出以下的异常：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/2.png">

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/3.png">

&emsp;&emsp;图3中抛出的异常对应的代码实际就是上文提到的[NumericDocValuesWriter](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/index/NumericDocValuesWriter.java)类中的：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/4.png">

&emsp;&emsp;上文中说到相同域名的NumericDocValuesField对应的NumericDocValues信息使用同一个NumericDocValuesWriter收集，在收集的过程中，如果一篇文档中包含了两个或以上相同域名的NumericDocValuesField，如图4所示，docID <= lastDocId的条件就会成立，即抛出图3中的异常。

&emsp;&emsp;在收集NumericDocValues信息的过程中，我们仅仅关心下面两个信息：

- docId：即包含NumericDocValues信息的文档号
- 域值：即NumericDocValuesField的域值，例如图1的文档0中，域名为"age"的NumericDocValuesField的域值为88

### docId

&emsp;&emsp;在源码中，使用[DocsWithFieldSet](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/index/DocsWithFieldSet.java)对象收集文档号docId，使得在**某个特殊条件**（见下文流程点`是否使用了FixedBitSet？`的介绍）下，**在索引阶段能更少的占用内存，在读取阶段有更好的读写性能**。

&emsp;&emsp;我们通过介绍DocsWithFieldSet存储文档号的的流程来解释上述的内容：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/5.png">

&emsp;&emsp;在介绍图5的流程之前，我们先介绍下图中几个变量：

- lastDocId：该值描述的是DocsWithFieldSet上一次处理的文档号
- cost：该值的初始值为0，它其中一个作用是描述DocsWithFieldSet已经处理的文档数量，其他的作用在下文中会介绍
- FixedBitSet：用于存储文档号，见文章[工具类之FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/45.html)的介绍

#### 文档号

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/6.png">

&emsp;&emsp;图5的流程描述了DocsWithFieldSet存储一个文档号的过程，所以流程图的准备数据为一个文档号。

#### 文档号是否不大于已收集的文档号？

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/7.png">

&emsp;&emsp;通过比较当前处理的文档号docId跟lastDocId的值来判断`文档号是否不大于已收集的文档号`，如果判断为否，那么直接抛出异常：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/8.png">

&emsp;&emsp;当前流程点判断的目的是想说明DocsWithFieldSet只处理从小到大有序的文档号集合。

#### FixedBitSet

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（十五）/9.png">

&emsp;&emsp;介绍图9的流程点之前，我们先说下DocsWithFieldSet存储文档号的两种方式：

- FixedBitSet：见文章[工具类之FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/45.html)
- cost：这里的cost即上文中提到的cost，它一方面描述了DocsWithFieldSet已经处理的文档数量，同时在**某个特殊条件**下，它也能用来描述DocsWithFieldSet存储的文档号集合（见下文介绍）

**某个特殊条件是什么？**

&emsp;&emsp;该特殊条件指的是DocsWithFieldSet处理的文档号集合中的文档号是从0开始有序递增的，这意味着段（段内的文档号是从0开始有序递增的）中每一篇文档中都包含某个域名的NumericDocValues信息。

&emsp;&emsp;例如图1中，如果添加了三篇文档后生成了一个段，那么对于域名为"age"的NumericDocValues信息就满足上述的特殊条件，而对于域名为"level"的NumericDocValues信息，由于文档1中没有添加这个域，那么就不满足。

**满足或者不满足特殊条件有什么不同**

&emsp;&emsp;如果满足特殊条件，意味着我们不需要存储每个文档号，只需要知道cost的值就行了，cost的默认值为0，每处理一个文档号，cost的值就执行+1操作，那么在读取文档号阶段我们就可以根据cost获得文档号集合区间，即[0, cost]。

&emsp;&emsp;如果不满足特殊条件，那么只能通过FixedBitSet来存储每一个文档号。

&emsp;&emsp;可见如果满足了特殊条件，在索引阶段，我们就不需要额外使用FixedBitSet对象来存储文档号，即上文中提到的**在索引阶段能更少的占用内存**；同时在读取阶段，我们只要顺序遍历0~cost的值就可以获取文档号，而不需要通过FixedBitSet来读取文档号（见文章[工具类之FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/45.html)），即上文中提到的**在读取阶段有更好的读写性能**。

&emsp;&emsp;回到当前流程点的介绍，如果流程点`是否使用了FixedBitSet？`为否，说明之前处理的文档号集合是从0开始有序递增的，如果为是，那么只能通过FixedBitSet存储文档号，即执行流程点`使用FixedBitSet存储文档号`，接着在流程点`是否新建FixedBitSet？`的判断中，通过比较当前处理的文档号docId跟cost值来进行判断：

- docId 等于cost：说明目前仍然处于满足特殊条件的情况，即流程点`是否新建FixedBitSet？`的判断为假，那么直接更新lastDocId的值更新为dociId，用于处理下一个文档号时，判断图7的流程点`文档号是否不大于已收集的文档号`，接着更新cost的值，即cost++的操作，在这个操作之后，通过上文中的介绍可以知道，目前DocsWithFieldSet已经处理（存储）的文档号集合为[0, cost]
- docId 不等于cost（docId与cost的差值大于1）：说明从当前处理的文档号开始就不满足特殊条件了，即流程点`是否新建FixedBitSet？`的判断为真（是），那么此时需要执行流程点`使用新建的FixedBitSet存储文档号`，同时将之前处理的文档号（通过cost的值获得）以及当前文档号存储到新建的FixedBitSet对象中。当然还得继续更新lastDocId，因为在处理下一个文档号时，lastDocId要用于判断图7的流程点`文档号是否不大于已收集的文档号`；cost的值依然执行cost++的操作，因为cost的功能不仅仅是用来判断在**特殊条件**下，用来描述存储的文档号集合，它还要用来描述DocsWithFieldSet处理的文档号数量（见上文cost的介绍）

### 域值

&emsp;&emsp;域值即NumericDocValuesField的域值，例如图1的文档0中，域名为"age"的域值为88，我们需要收集88这个域值。

&emsp;&emsp;源码中使用了PackedLongValues来实现压缩存储，关于PackedLongValues的内容请参看文章[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)，本文不赘述，在后面的文章中，会再次介绍PackedLongValues的内容。

## 结语

&emsp;&emsp; 无

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的生成/索引文件的生成（十五）/索引文件的生成（十五）.zip)下载附件