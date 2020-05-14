# [IndexedDISI（一）](https://www.amazingkoala.com.cn/Lucene/gongjulei/)（Lucene 8.4.0）

&emsp;&emsp;IndexedDISI工具类在Lucene中用来存储[Norm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0305/39.html)/[DovValues](https://www.amazingkoala.com.cn/Lucene/DocValues/)对应的文档号，其实现原理借鉴了roaring bitmaps（见文章[RoaringDocIdSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/1008/98.html)），本文先通过介绍在Lucene7.5.0中的实现来理解其原理，接着会介绍在Lucene8.4.0中的优化实现。

## IndexedDISI写入文档号

### Block

&emsp;&emsp;使用IndexDISI存储的数据结构如下所示：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（一）/1.png">

&emsp;&emsp;图1中，每个block用来描述**最多**2^16个文档号信息，例如第一个block中描述的文档号集合为[0, 2^16 - 1]，在处理某个文档号时，根据下面的公式来找到存储该文档号对应的block：

```java
int block = docId >>> 16
```

&emsp;&emsp;如果当前处理的文档号为 3，那么根据上面的公式 block = 3 >>> 16 = 0，那么文档号3将被存储在第一个block中，如果当前处理的文档号为 65538，根据上面的公式 block = 65538 >>> 16 = 1，那么文档号65538将被存储在第二个block中。

### 稠密度（density）

&emsp;&emsp;在一个block中，根据block中存储的文档号数量划分为三种稠密度：

- ALL：block中存储的文档号数量为2^16个
- DENSE：block中存储的文档号数量范围为 [4096, 2^16 - 1]，使用[FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/45.html)存储文档号
- SPARSE：block中存储的文档号数量范围为 [1, 4095]，使用short类型数组存储文档号

&emsp;&emsp;存储文档号使用的数据结构根据稠密度各不相同，我们只介绍介绍DENSE跟SPARSE，至于为什么不介绍稠密度ALL，我们将在系列文章[索引文件的生成（十五）之dvm&&dvd](https://www.amazingkoala.com.cn/Lucene/Index/2020/0507/139.html)中作出解释。

#### DENSE

&emsp;&emsp;该稠密度对应的block的数据结构如下所示：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（一）/2.png">

##### BlockId

&emsp;&emsp;BlockId为block的编号，例如图1中，第一个block的编号为0，第二个block的编号为2。

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（一）/3.png">

&emsp;&emsp;上文中我们说到，每个block中描述了2^16个文档号，另外通过图3可知，blockId最大值为65535，可见IndexDISI最多描述2^32个文档号。

&emsp;&emsp;**BlockId的作用**：

&emsp;&emsp;blockId用于计算baseId，计算公式为：

```java
baseId = (blockId * 65536)
```

&emsp;&emsp;在使用了baseId后，在每个block中只要存储块内文档号就行了，意味着，在一个block中存储的文档号的取值范围为 0 ~ 65535，在读取阶段， 通过blockId跟块内文档号就可以获得原始文档号。

##### docIdNumber

&emsp;&emsp;docIdNumber为文档数量，它描述了当前block中存储的文档号数量。

##### word

&emsp;&emsp;word是一个long类型的值，long类型的数值占用64个bit，每一个bit用来描述一个文档号，另外上文中说到block中最多描述2^16个文档号，那么图2中word的数量就是固定的 65536/64，即1024个。

&emsp;&emsp;对于blockId为0的block中的第一个word，该word描述的文档号范围为 0 ~ 63，第二个word描述的文档号范围为 64 ~ 127，看下面的例子：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（一）/4.png">

&emsp;&emsp;图4中，block的编号为0的第一个word中存储了3个文档号{3, 57, 60}，如果没看懂，建议先阅读文章[工具类之FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/45.html) 。

#### SPARSE

&emsp;&emsp;该稠密度对应的block的数据结构如下所示：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（一）/5.png">

&emsp;&emsp;稠密度为SPARSE时，文档号直接用short类型数组存储即可，在上文中说到，块内文档号的取值范围为0 ~ 65535，那么文档号的最大值需要16bit才能表示，而short类型的值占用2个字节，故采用short类型的数组存储。

## IndexedDISI读取文档号

&emsp;&emsp;我们接着介绍IndexedDISI读取文档号的过程，在介绍了其读取过程后就可以明白为什么从Lucene8.0.0版本开始，对IndexedDISI的写入进行了优化。

&emsp;&emsp;在Lucene最常用的应用中，我们在[Collector](https://www.amazingkoala.com.cn/Lucene/Search/2019/0812/82.html)中会对满足查询条件的文档号进行排序，排序规则通常使用DocValues来实现，而包含DocValues信息的文档的文档号就使用IndexedDISI存储，故在排序过程中，先判断这个满足查询的文档号是否包含了DocValues信息，该过程即在IndexedDISI中读取文档号，判断是否存在这个文档号。

&emsp;&emsp;判断一个文档号是否在IndexedDISI中可以简单的划分为两个步骤：

- 步骤一：找到所属block
- 步骤二：判断block是否包含此文档号

### 步骤一：找到所属block

&emsp;&emsp;通过下面的公式先找到文档号属于哪一个block：

```java
int block = docId >>> 16
```

&emsp;&emsp;该过程也就是计算出上文中提到的block编号blockId，通过逐个比较block中的BlockId字段找到所属block，可见找到所属block的时间复杂度为O(n)，如下所示：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/IndexedDISI/IndexedDISI（一）/6.png">

### 步骤二：判断block是否包含此文档号

&emsp;&emsp;当找到所属block之后，我们继续在块内寻找，根据不同的稠密度，查找方式也各不相同。

#### DENSE

&emsp;&emsp;这种稠密度使用了word来存储文档号，我们通过下面的公式可以计算出查询的文档号应该在第n个word中：

```java
docId >>> 6
```

&emsp;&emsp;注意的是此时docId是块内文档号，最后通过与操作就可以判断是否包含此文档号（看不懂？说明你没有看文章[工具类之FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/45.html) ）。

&emsp;&emsp;在计算应该在第n个word的过程中，我们需要做一件事情，那就是必须知道当前查询的文档号它是IndexedDISI存储的第几个文档号（可能不包含当前查询的文档号），我们称之为 段内编号，基于上文中的存储方式，我们只能通过累加前n -1 个word中的文档数量来获得，意味着我们必须依次处理每一个word，故时间复杂度为O(n)。

&emsp;&emsp;**为什么要获得当前查询的文档号在block中的段内编号**

&emsp;&emsp;因为如果block中有这个当前查询的文档号，那么我们还要取出DocValues的值，才能在Collector中进行排序比较，并且需要通过段内编号才能找到当前查询文档号对应的DocValues的值，这块内容的介绍将在系列文章[索引文件的生成（十五）之dvm&&dvd](https://www.amazingkoala.com.cn/Lucene/Index/2020/0507/139.html)中作出解释。

#### SPARSE

&emsp;&emsp;这种稠密度就是简单的遍历short[ ]数组来判断，不赘述。

## 结语

&emsp;&emsp;通过上述的介绍，可以发现两个步骤的时间复杂度均为O(n)，在Lucene8.0.0之后，会通过查找表（lookup table）提高查询性能，基于篇幅将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/utils/IndexedDISI/IndexedDISI（一）/IndexedDISI（一）.zip)下载附件