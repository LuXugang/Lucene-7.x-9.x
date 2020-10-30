# [索引文件的生成（二十四）](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.6.0）

&emsp;&emsp;本文承接文章[索引文件的生成（二十三）之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1015/170.html)，继续介绍剩余的内容，先给出生成索引文件fdx&&fdt&&fdm的流程图：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/1.png">

## 索引阶段

### 是否生成一个chunk

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/2.png">

&emsp;&emsp;在上一篇文章中我们知道，在索引阶段，每处理一篇文档会将存储域的域值写入到bufferedDoc[ ]数组中并且增量统计存储域的信息，其中一个信息就是numBufferedDocs，它描述了在生成一个chunk之前，当前已经处理的文档数量。当前流程点的判断条件就依赖上文的描述，由于该条件的判断方式比较简单，故直接给出源码：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/3.png">

&emsp;&emsp;图3中，bufferedDocs.getPosition( )方法描述了存储在bufferedDoc[ \]数组中的域值长度（占用字节数量），chunkSize是一个阈值，达到阈值后即满足第一种生成一个chunk的条件，chunkSize的值有两个可选项：16384跟61440，他们分别对应两种生成索引文件.fdt的模式：FAST跟HIGH_COMPRESSION模式，这两种模式描述了对bufferedDoc[ \]数组中的域值使用不同的压缩算法，分别是[LZ4](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0226/37.html)跟JDK中的Deflater。生成索引文件.fdt的模式可以通过实现抽象类[Codec](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/codecs/Codec.java)来自定义。

&emsp;&emsp;第二种满足生成的条件为$numBufferedDocs >= maxDocsPerChunk$，其中maxDocsPerChunk也是一个阈值，同样有两个可选项：128跟512，分别对应上文中的生成索引文件.fdt的模式。

&emsp;&emsp;故图3中满足流程点`是否生成一个chunk`的条件就是，目前处理的域值长度或者处理的文档数量是否超过阈值。

### 生成一个chunk

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/4.png">


&emsp;&emsp;在当前流程点`生成一个chunk`中，我们将把目前已经处理的信息（域值信息、存储域的信息）生成一个chunk，完成下面几个工作：

- 记录chunk的两个信息
- docBase、numBufferedDocs写入到索引文件.fdt中
- 存储域的信息写入到索引文件.fdt中
- 域值信息写入到索引文件.fdt中



#### 记录chunk的两个信息

&emsp;&emsp;此流程中会使用两个临时文件记录这个即将生成的chunk的两个信息：

- chunk在索引文件.fdt中起始读取位置
- chunk中的文档数量

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/5.png">

&emsp;&emsp;图5中的两个临时文件会统计**一个段**中所有chunk的两个信息，在图1的flush阶段，将分别用于`生成NumDocs的信息`、`生成startPoints的信息`这两个流程点。

&emsp;&emsp;至于chunk的这两个信息的作用，我们随后将在`索引文件的读取之fdx&&fdt&&fdm`的文章中详细展开。

#### docBase、numBufferedDocs写入到索引文件.fdt中

&emsp;&emsp;docBase是chunk中第一个文档的**段内文档号**、将被写入到索引文件.fdt的DocBase字段，numBufferedDocs将被写入到索引文件.fdt的ChunkDos字段，ChunkDos字段中还包含了sliceBit，该值是一个布尔类型，在下文中会具体介绍，故numBufferedDocs和sliceBit将作为一个组合值被写入到ChunkDos字段中，组合方式为$(numBufferedDocs << 1 )| sliceBit$，即ChunkDos的最低位用来存储布尔类型的sliceBit，如下所示：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/6.png">

#### 存储域的信息写入到索引文件.fdt中

&emsp;&emsp;在文章[索引文件的生成（二十三）之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1015/170.html)中我们知道，存储域的信息在内存中使用numStoredFields[ ]数组，endOffsets[ ]数组存储，他们分别将被写入到DocFieldCounts字段跟DocLengths字段，如下所示：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/7.png">

&emsp;&emsp;numStoredFields[ \]数组，endOffsets[ \]数组中的信息被写入到索引文件.fdt之前，会先进行编码压缩处理，不同的处理方式使得DocFieldCounts、DocLengths有不同的数据结构，该内容在文章[索引文件之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)已经介绍，不赘述。

#### 域值信息写入到索引文件.fdt中

&emsp;&emsp;在文章[索引文件的生成（二十三）之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1015/170.html)中我们知道、域值信息在内存中使用bufferedDocs[ ]数组存储，它将被写入到CompressedDocs字段，如下所示：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/8.png">

&emsp;&emsp;域值信息被写入到索引文件.fdt之前，会根据域值的总长度（占用字节数量）判断是否需要进行切片压缩，判断条件如下：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/9.png">

&emsp;&emsp;图9中，当域值长度满足条件后，需要对域值进行切片压缩，将域值按照chunkSize划分为每一个切片，然后对每个切片使用[LZ4算法（上）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0226/37.html)算法进行压缩处理。

&emsp;&emsp;至此，在索引阶段生成索引文件fdx&&fdt&&fdm的流程点介绍完毕。从上文的介绍可以发现，有可能存在这么一种情况，添加完最后一篇文档后，还未满足图2中`是否生成一个chunk`的条件，也就是说，还有一些文档的域值信息、存储域信息未被写入到索引文件.fdt中，那么这些未处理的文档将会在flush阶段完成。

## flush阶段

&emsp;&emsp;在flush阶段，一方面将处理上文中说的未处理的文档的域值信息、存储域信息，另一方面还会根据图5中的两个临时文件中的信息生成索引文件.fdx、fdm。

&emsp;&emsp;图1中flush阶段的流程在执行[flush](https://www.amazingkoala.com.cn/Lucene/Index/2019/0716/74.html)（IndexWriter.flush()）的流程中的位置如下所示：

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/10.png">

[点击](https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/flush.html)查看大图

&emsp;&emsp;注意的是图10中除了图1中的流程，其他的流程都是基于Lucene 7.5.0，与Lucene 8.6.0可能会有些差异，但是生成索引文件fdx&&fdt&&fdm在flush阶段的时机点是相同的。

### 生成一个chunk

&emsp;&emsp;这里生成一个chunk的逻辑跟图4中的流程点是一样，区别在于当前即将生成的chunk中的域值长度以及文档数量是不满足图2中的条件的，在执行完当前流程后，会统计增量统计一个变量numDirtyChunks，它随后将被写入到索引文件.fdt中。

### 生成NumDocs的信息

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/11.png">

&emsp;&emsp;在图1的索引阶段，使用了图5中的临时文件\_0\_Lucene85FieldsIndex-doc\_ids\_0.tmp存储了一个段中每个chunk中包含的文档数量，从上文的描述我们可以知道，一个chunk中的文档数量可能因为域值的长度而各不相同。

&emsp;&emsp;处理过程为读取每一个chunk中的文档数量，每处理1024（2 << blockshift（blockshift的介绍见文章[索引文件之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)））个chunk就生成一个NumDocsBlock，然后写入到索引文件.fdx中：

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/12.png">

&emsp;&emsp;图12中NumDoc字段为某个chunk中的文档数量，最后NumDocsBlock会**先进行编码再使用[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)进行压缩处理**后再写入到索引文件.fdx中。

&emsp;&emsp;上文中说到每处理2 << blockshift个chunk就生成一个NumDocsBlock，此时还会生成NumDocMeta信息来记录编码信息，使得在读取阶段能通过NumDocMeta中的编码信息恢复成原数据NumDoc，NumDocMeta信息会被写入到索引文件.fdm中：

图13：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/13.png">

&emsp;&emsp;图13中Min、AvgInc、Offset、BitRequired为编码信息，其编码逻辑本文不会展开，编码的目的是为了能降低索引文件的文件大小，另外图13中的其他字段的介绍可以阅读文章[索引文件之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)。


### 生成StartPoints的信息

图14：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/14.png">

&emsp;&emsp;在图1的索引阶段，使用了图5中的临时文件\_0\_Lucene85FieldsIndexfile\_pointers\_1.tmp存储了一个chunk对应的数据块在索引文件.fdt中的起始读取位置。

&emsp;&emsp;处理过程为读取每一个chunk中的对应的数据块在索引文件.fdt中起始读取位置，每处理1024（2 << blockshift（blockshift的介绍见文章[索引文件之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)））个chunk就生成一个StartPointBlock，然后写入到索引文件.fdx中：

图15：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/15.png">

&emsp;&emsp;图15中StartPoint为某个chunk对应的数据块在索引文件.fdt中的起始读取位置。同样的，StartPointBlock会**先进行编码再使用[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)进行压缩处理**后再写入到索引文件.fdx中。 

&emsp;&emsp;跟NumDocsBlock一样，每处理2 << blockshift个chunk就生成一个StartPointBlock，此时还会生成StartPointMeta信息来描述编码信息，使得在读取阶段能通过StartPointMeta中的编码信息恢复成原数据StartPoint，StartPointMeta信息会被写入到索引文件.fdm中：

图16：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十四）/16.png">

&emsp;&emsp;图16中Min、AvgInc、Offset、BitRequired为编码信息，其编码逻辑本文不会展开。另外图13中的其他字段的介绍可以阅读文章[索引文件之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)。

## 结语

&emsp;&emsp;至此，生成**索引文件fdx&&fdt&&fdm**过程介绍完毕，在随后介绍**读取索引文件fdx&&fdt&&fdm**文章中，我们将对比Lucene 7.5.0版本的索引文件fdx&&fdt，介绍数据结构的差异以及优化。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的生成/索引文件的生成（二十四）/索引文件的生成（二十四）.zip)下载附件





