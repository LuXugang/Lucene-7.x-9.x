---
title: 索引文件之fdx&&fdt（Lucene 7.5.0）
date: 2019-03-01 00:00:00
tags: [index, indexFile,fdx,fdt]
categories:
- Lucene
- suoyinwenjian
---

&emsp;&emsp;当STORE.YES的域生成了[倒排表](https://www.amazingkoala.com.cn/Lucene/Index/2019/0222/倒排表（上）)以后，将文档的域值信息写入到.fdt（field data）、.fdx（field index）文件中。
# 数据结构
## .fdt

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/1.png">
### ChunkSize

&emsp;&emsp;ChunkSize作为一个参数，用来判断是否要生成一个Chunk，以及用来描述压缩存储域值信息的方式，后面会详细介绍。

### PackedIntsVersion

&emsp;&emsp;PackedIntsVersion描述了压缩使用的方式，当前版本中是VERSION_MONOTONIC_WITHOUT_ZIGZAG。

### Chunk

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/2.png">

&emsp;&emsp;在处理每一篇文档的过程中，如果满足以下任意一个条件，那么就将已处理的文档的域值信息生成一个chunk：

- 已处理的文档数量达到128
- 已处理的所有域值的总长度达到ChunkSize。

#### DocBase

&emsp;&emsp;当前chunk中第一个文档的文档号（该文档号为段内文档号），因为根据这个文档号来差值存储，在读取的阶段需要根据该值恢复其他文档号。
#### ChunkDocs

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/3.png">

&emsp;&emsp;ChunkDocs是一个numBufferedDocs跟slicedBit的组合值。ChunkDocs = (numBufferedDocs |slicedBit )。

##### numBufferedDocs

&emsp;&emsp;numBufferedDocs描述了当前chunk中的文档数量。numBufferedDocs是一个 ≤ 128的值。

##### slicedBit

&emsp;&emsp;如果待处理的域值信息的长度超过2倍的chunkSize（默认值 16384），那么需要分块压缩，下文会具体介绍。
#### DocFieldCounts

&emsp;&emsp;根据chunk中包含的文档个数numBufferedDocs、每篇文档包含的存储域的数量numStoredFields分为不同的情况。

##### numBufferedDocs的个数为1

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/4.png">
##### numBufferedDocs的个数＞ 1 并且每篇文档中的numStoredFields都是相同的
图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/5.png">

&emsp;&emsp;只要存储一个numStoredFields的值就行啦。

##### numBufferedDocs的个数＞ 1 并且每篇文档中的numStoredFields不都相同的

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/6.png">

&emsp;&emsp;使用PackedInt来存储所有的numStoredFields，这里不赘述了，[点击这里](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0213/BulkOperationPacked)可以看其中的一种压缩方式。

#### DocLengths

&emsp;&emsp;同DocFieldCounts类似，据chunk中包含的文档个数numBufferedDocs、每篇文档中域值信息的长度分为不同的情况。

##### numBufferedDocs的个数为1

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/7.png">

##### numBufferedDocs的个数＞ 1 并且每篇文档中的域值信息长度都是相同的

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/8.png">
##### numBufferedDocs的个数＞ 1 并且每篇文档中的域值信息长度不都是相同的

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/9.png">

&emsp;&emsp;使用PackedInt来存储所有的域值信息长度，这里不赘述了，[点击这里](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0213/BulkOperationPacked)可以看其中的一种压缩方式。

#### CompressedDocs

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/10.png">

&emsp;&emsp;CompressedDocs中使用[LZ4](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0226/LZ4)算法将域值信息压缩存储。域值信息包含如下内容，字段Doc的数量对应为一个chunk中包含的文档数量：

- 域的编号
- 域值的类型：String、BinaryValue、Int、Float、Long、Double
- 域值的编号跟域值的类型组合存储为FieldNumAndType
- Value：域值

## .fdt整体数据结构

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/11.png">

&emsp;&emsp;上图中是其中一种 .fdt文件数据结构。

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/12.png">

### Block

图13：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/13.png">

&emsp;&emsp;在.fdt中，每当chunk的个数达到1024(blockSzie)，在.fdx文件中就会生成一个block，block中的信息作为索引来映射.fdt中的数据区间。

#### BlockChunks

&emsp;&emsp;block中包含的chunk的个数，即1024个。

#### DocBases

图14：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/14.png">

&emsp;&emsp;DocBases中描述了文档号的信息。

##### DocBase

&emsp;&emsp;block中第一个文档的文档号。用来在读取阶段，恢复所有chunk中其他被编码的文档号。

##### AvgChunkDocs

&emsp;&emsp;AvgChunkDocs描述了block中平均一个chunk中包含的文档数。

##### BitsPerDocBaseDelta

&emsp;&emsp;BitsPerDocBaseDelta描述了存储文档号的需要固定bit个数。

##### DocBaseDeltas

&emsp;&emsp;一个block中用docBaseDeltas[]数组来存放每个chunk中的文档个数，而每一个chunk中的文档个数是不一样的，出于最大化优化空间存储，不直接对文档数量值进行存储，而是存储差值docDelta。又因为docBaseDeltas[]数组又不能保证数组元素递增，所以不能使用相邻数组元素的差值来作为docDelta，Lucene提供的方法就是计算docBaseDeltas[]中数组元素平均值avgChunkDocs，对每一个数组元素存储一个docDelta的值，docDelta的计算公式为：docDelta = ( docBase - avgChunkDocs * i), 其中i为数组的下标值，docBase是下标值为i的数组元素前所有的数组元素之和，然后对所有docDelta使用PackedInts进行压缩编码，即DocBaseDeltas。
#### StartPointers

图15：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/15.png">

&emsp;&emsp;StartPointers中描述了.fdt文件中每一个chunk的索引映射信息。

##### StartPointerBase

&emsp;&emsp;当前block中第一个chunk的索引值。
##### AvgChunkSize

&emsp;&emsp;block中平均每一个chunk的大小。

##### BitsPerStartPointerDelta

&emsp;&emsp;存储每一个chunk大小需要固定bit个数。

##### StartPointerDeltas

&emsp;&emsp;逻辑跟DocBaseDeltas一样，不赘述。

### ChunkCount

chunk的个数。

### DirtyChunkCount

在索引阶段，如果还有一些文档未被写入到索引文件中，那么在flush阶段会强制写入，并用该字段记录。

## .fdx整体数据结构

图16：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/fdt&&fdx/16.png">

# 结语
&emsp;&emsp;没啥要讲的。

[点击下载](http://www.amazingkoala.com.cn/attachment/Lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fdx&&fdt.zip)Markdown文件