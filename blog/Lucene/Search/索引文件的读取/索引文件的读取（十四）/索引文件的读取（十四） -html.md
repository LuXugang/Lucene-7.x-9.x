# [索引文件的读取（十四）](https://www.amazingkoala.com.cn/Lucene/Search/)（Lucene 8.6.0）

&emsp;&emsp;在前几篇索引文件的读取的系列文章中，我们介绍[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/43.html)的读取时机点时说到，在生成[StandardDirectoryReader](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)对象期间，会生成[SegmentReader](https://www.amazingkoala.com.cn/Lucene/Index/2019/1014/99.html)对象，该对象中的StoredFieldsReader信息描述了[索引文件fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)中所有域的索引信息，故我们从本篇文章开始介绍索引文件fdx&&fdt&&fdm的读取。

## StoredFieldsReader

&emsp;&emsp;在生成StandardDirectoryReader阶段，就已经开始读取索引文件fdx&&fdt&&fdm实现一些初始化，为随后的搜索阶段做准备，读取后的信息用StoredFieldsReader来描述。

### .fdm

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/1.png">

&emsp;&emsp;.fdm文件中存储了元数据，即描述存储域数据的信息，它包含的所有数据将被**完整的读入到内存**中。

### .fdx

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/2.png">

&emsp;&emsp;通过索引文件.fdm中的**StartPointsIndex与NumDocsIndex的差值**来确定在索引文件.fdx中的一段数据区间，该区间中存储了chunk中的文档数量，同理**SPEndPointer与StartPointsIndex的差值**确定在索引文件.fdx中的一段数据区间，该区间中存储了每个chunk在索引文件.fdt中的起始读取位置。

&emsp;&emsp;注意的是，在生成StandardDirectoryReader阶段，图2中NumDoc跟StartPoints的字段的值**并没有读取到内存**中，只是将这两块数据的起始读取位置读取到了内存，相比较在Lucene 8.5.0之前的版本，这种读取方式即所谓的"off-heap"。

#### off-heap

&emsp;&emsp;在Lucene 8.5.0之前，描述存储域的索引文件为.fdx、.fdt，它们的数据结构完整介绍可以见文章[索引文件之fdx&&fdt](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0301/38.html)，本文中中我们暂时只给出索引文件.fdx来介绍"off-heap"：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/3.png">

&emsp;&emsp;在Lucene 8.5.0之前，图3中**所有的Block**会在生成StandardDirectoryReader阶段就**全部读取到内存**中，注意的是图3中的DocBases对应图2中NumDocs，命名不同而已，描述的内容是一致。

&emsp;&emsp;此次优化的详细内容可以见这个issue的介绍：https://issues.apache.org/jira/browse/LUCENE-9147 。在文章[索引文件的读取（七）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Search/2020/0804/158.html)中我们提到，索引文件.tip的读取也是用了off-heap，并且在Lucene 8.0.0就早早实现了，为什么在Lucene 8.5.0之后才将存储域的索引文件的读取使用off-heap呢？原因有两点，直接贴出issue原文：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/4.png">

&emsp;&emsp;图4的大意就是，在terms index（索引文件.tip）使用了off-heap之后，存储域（stored fields）的索引文件变成了占用内存的大头，但是它没有terms index那样对性能有很大的影响（见文章[索引文件的读取（七）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Search/2020/0804/158.html)）。

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/5.png">

&emsp;&emsp;图5中的更有意思，[Erick Erickson](https://github.com/ErickErickson)说当在技术层面（technical aspects）无法创新时，那么从内存方面去考虑优化了。

### .fdt

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/6.png">

&emsp;&emsp;对于索引文件.fdt，在此阶段通过索引文件.fdm的maxPointer，读取出ChunkCount、DIrtyChunkCount以及ChunkSize、PackedIntsVersion字段而已，也就是说Chunk字段，占用内存最大的数据块，没有被读取到内存中，在搜索阶段才根据条件读取，下文中中会展开介绍。

## 读取索引文件fdx&&fdt&&fdm的流程图

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/7.png">

### 准备数据

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/8.png">

&emsp;&emsp;准备数据是**全局的文档号**。该文档号就是满足搜索条件的文档对应的文档号，例如下图中,ScoreDoc[ ]对象中存放的就是满足查询条件的全局的文档号。

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/9.png">

### 计算出所属段并转化为段内文档号

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/10.png">

&emsp;&emsp;在生成[StandardDirectoryReader](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)对象期间，通过获取每个段中的文档数量，会初始化一个int类型的starts[ \]数组，随后根据这个数据就可以计算出某个全局文档号属于哪一个段。通过读取[索引文件.si](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0605/63.html)中的<font color=Red>segSize</font>字段来获取每个段中的文档数量，如下所示：

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/11.png">

&emsp;&emsp;我们直接以一个例子来介绍starts[ \]数组：

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/12.png">

&emsp;&emsp;图12的例子，每当count达到1000、3000、20000、100000、结束时就生成一个段，即索引目录中存在5个段。那么对应的starts[\]数组如下所示：

图13：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/13.png">

&emsp;&emsp;那么根据starts[ \]数组，通过二分法就能计算出某个全局文档号所属的段了。

&emsp;&emsp;另外starts[ \]数组中的元素还代表了某个段的段内的第一篇文档号，那么将全局文档号与之做减法就获得了段内文档号。**注意的是，为了便于介绍，下文中出现的文档号都是段内文档号**。

### 文档号是否在BlockState中？

图14：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/14.png">

&emsp;&emsp;BlockState中存储的是当前正在读取的Chunk的一些元数据，至少包含以下的内容：

- docBase
- chunkDocs
- sliced
- offsets[ ]数组
- numStoredFields[ ]数组

&emsp;&emsp;这些元数据对应在索引文件中内容如下所示：

图15：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十四）/15.png">

&emsp;&emsp;通过docBase跟chunkDocs就可以判断当前chunk中是否包含某个文档号。

&emsp;&emsp;如果当前chunk中包含某个文档号，那么直接读取该Chunk即可，否则需要**重新**从.fdt中找到所属chunk（查找过程将在下一篇文中展开），同时更新BlockState，继而获得文档中的存储域信息。从这里我们可以看出，在实际使用过程中，获取存储域的信息时，最好按照全局的文档号的大小依次获取，随机的文档号会导致频繁的更新blockState，也就是需要从索引文件中不断的读取Chunk，即增加了I/O开销。

## 结语

&emsp;&emsp;基于篇幅，剩余的内容将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/索引文件的读取（十四）/索引文件的读取（十四）.zip)下载附件













