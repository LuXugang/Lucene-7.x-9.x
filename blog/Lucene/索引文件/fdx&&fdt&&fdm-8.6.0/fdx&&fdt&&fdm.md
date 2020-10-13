# [fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/)（Lucene 8.6.0）

&emsp;&emsp;在索引阶段，如果某个域的属性中包含store，意味着该域的域值信息将被写入到索引文件fdx&&fdt&&fdm中，域的属性可以通过FieldType来设置，如下所示：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/1.png">

&emsp;&emsp;图1中，域"content"跟域"title"的域值将被存储，即写入到索引文件fdx&&fdt&&fdm中，而域”attachment“则不会。

&emsp;&emsp;在文章[索引文件之fdx&&fdt](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0301/38.html)中介绍了Lucene 7.5.0版本存储域值对应的索引文件，该版本中使用了两个索引文件.fdx、.fdt存储域值信息，而从Lucene 8.5.0版本开始进行了优化，最终用三个索引文件.fdx、fdt、fdm三个索引文件来存储域值信息，其优化的目的以及方式不会在本文中提及，随后在介绍生成这三个索引文件的生成过程的文章中再详细展开，并会跟Lucene 7.5.0版本进行对比，本文只对索引文件中的字段作介绍。

# 数据结构

## 索引文件.fdt

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/2.png">

### ChunkSize

&emsp;&emsp;ChunkSize作为一个参数，用来判断是否要生成一个Chunk，以及用来描述压缩存储域值信息的方式，后面会详细介绍。

### PackedIntsVersion

&emsp;&emsp;PackedIntsVersion描述了压缩使用的方式，当前版本中是VERSION_MONOTONIC_WITHOUT_ZIGZAG。

### Chunk

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/3.png">

&emsp;&emsp;在处理文档的过程中，如果满足以下任意一个条件，那么将已处理的文档的域值信息生成一个chunk：

- 已处理的文档数量达到128
-  已处理的所有域值的总长度达到ChunkSize。

#### DocBase

&emsp;&emsp;当前chunk中第一个文档的文档号（该文档号为段内文档号），因为根据这个文档号来差值存储，在读取的阶段需要根据该值恢复其他文档号。

#### ChunkDocs

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/4.png">

&emsp;&emsp;ChunkDocs是一个numBufferedDocs跟slicedBit的组合值。ChunkDocs = (numBufferedDocs |slicedBit )。

##### numBufferedDocs

&emsp;&emsp;numBufferedDocs描述了当前chunk中的文档数量。numBufferedDocs是一个 ≤ 128的值。

##### slicedBit

&emsp;&emsp;如果待处理的域值信息的长度超过2倍的chunkSize（默认值 16384），那么需要分块压缩，下文会具体介绍。

#### DocFieldCounts

&emsp;&emsp;根据chunk中包含的文档个数numBufferedDocs、每篇文档包含的存储域的数量numStoredFields分为不同的情况。

##### numBufferedDocs的个数为1

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/5.png">

##### numBufferedDocs的个数＞ 1 并且每篇文档中的numStoredFields都是相同的

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/6.png">

&emsp;&emsp;只要存储一个numStoredFields的值就行啦。

##### numBufferedDocs的个数＞ 1 并且每篇文档中的numStoredFields不都相同的

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/7.png">

&emsp;&emsp;使用PackedInt来存储所有的numStoredFields，这里不赘述了，[点击这里](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0213/31.html)可以看其中的一种压缩方式。

#### DocLengths

&emsp;&emsp;同DocFieldCounts类似，据chunk中包含的文档个数numBufferedDocs、每篇文档中域值信息的长度分为不同的情况。

##### numBufferedDocs的个数为1

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/8.png">

##### numBufferedDocs的个数＞ 1 并且每篇文档中的域值信息长度都是相同的

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/9.png">

##### numBufferedDocs的个数＞ 1 并且每篇文档中的域值信息长度不都是相同的

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/10.png">

&emsp;&emsp;使用PackedInt来存储所有的域值信息长度，这里不赘述了，[点击这里](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0213/31.html)可以看其中的一种压缩方式。

#### CompressedDocs

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/11.png">

&emsp;&emsp;CompressedDocs中使用[LZ4](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0226/37.html)算法将域值信息压缩存储。域值信息包含如下内容，字段Doc的数量对应为一个chunk中包含的文档数量：

- 域的编号
- 域值的类型：String、BinaryValue、Int、Float、Long、Double
- 域值的编号跟域值的类型组合存储为FieldNumAndType
- Value：域值
### ChunkCount

&emsp;&emsp;chunk的个数。

### DirtyChunkCount

&emsp;&emsp;在索引阶段，如果还有一些文档未被写入到索引文件中（未满足生成chunk的条件的文档），那么在flush阶段会强制写入，并用该字段记录。

### .fdt整体数据结构

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/12.png">


## 索引文件.fdx

图13：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/13.png">

&emsp;&emsp;在索引文件.fdx中，每处理1024（blockShitf参数，下文会介绍）个图3中的chunk，就将chunk中的信息就生成一个DocBaseBlock和StartPointBlock，多个DocBaseBlock和多个StartPointBlock分别组成DocBases字段以及StartPoints字段，注意的是最后一个DocBaseBlock和StartPoints中的chunk的信息数量可能不足1024个。

### DocBaseBlock

&emsp;&emsp;该字段中的数据使用了[PackedInts](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)进行压缩。

图14：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/14.png">

#### DocBase

&emsp;&emsp;DocBase为每个chunk中第一个文档的文档号（段内文档号）。

### StartPointBlock

&emsp;&emsp;该字段中的数据使用了PackedInts进行压缩。

图15：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/15.png">

#### StartPoint

&emsp;&emsp;StartPoint描述了图2中每个chunk的信息在索引文件.fdt中的起始位置。

### .fdx整体数据结构

图16：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/16.png">

## 索引文件.fdm

图17：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/17.png">

&emsp;&emsp;索引文件.fdm中的信息用于在读取阶段映射索引文件.fdx中的信息。

### NumDocs

&emsp;&emsp;该字段描述了包含存储域的文档数量。

### BlockShift

&emsp;&emsp;该字段用来描述在索引文件.fdx中，当处理2 << BlockShitf个chunk后就生成一个DocBaseblock或StartPointBlock，默认是10。

### TotalChunks

&emsp;&emsp;该字段描述了图2中Chunk的数量。

### DocBasesIndex

&emsp;&emsp;该字段描述了图16中DocBases字段的信息在索引文件.fdx的起始读取位置

### DocBasesMeta

&emsp;&emsp;该字段由多个DocVaseMeta组成，其数量最多是1024（2 << BlockShitf），由于图14中的DocBaseBlock中的信息在存储过程中执行了先编码后压缩的操作（后面的文章中会展开介绍），DocBaseMeta中存储了一些参数，使得在读取阶段能读取到DocBase的原始值。

图18：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/18.png">

&emsp;&emsp;图18中Min、AvgInc、Length、BitRequired字段的值作为参数，将会在读取阶段用于解码图14中的DocBaseBlock中的信息，本文中不展开对这些参数的介绍。

### StartPointsMeta

&emsp;&emsp;该字段由多个StartPointMeta组成，其数量最多是1024（2 << BlockShitf），由于图15中的StartPointBlock中的信息在存储过程中执行了先编码后压缩的操作（后面的文章中会展开介绍），StartPointMeta中存储了一些参数，使得在读取阶段能读取到StartPoint的原始值。

图19：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/19.png">

&emsp;&emsp;图18中Min、AvgInc、Length、BitRequired字段的值作为参数，将会在读取阶段用于解码图15中的StartPointBlock中的信息，本文中不展开对这些参数的介绍。

### .fdm整体数据结构

图20：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdx&&fdt&&fdm/20.png">

# 结语

&emsp;&emsp;看完这篇文章后，如果感到一脸懵逼， 木有关系，在随后的文章将会详细介绍索引文件fdx&&fdt&&fdm的生成过程。

[点击下载](http://www.amazingkoala.com.cn/attachment/Lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fdx&&fdt&&fdm.zip)Markdown文件