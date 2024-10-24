---
title: 索引文件之liv（Lucene 7.5.0）
date: 2019-04-25 00:00:00
tags: [index, indexFile,liv,delete,softDeletes]
categories:
- Lucene
- suoyinwenjian
---

索引文件.liv只有在一个segment中包含被删除的文档时才会生成，它记录了当前段中没有被删除的文档号。这里不会讨论一个segment是如何获得被删除的文档号，在后面的文章中，介绍IndexWriter.flush()时会详细介绍，本篇文章只介绍那些被删除的文档号生成的索引文件的数据结构。
# 预备知识
介绍.liv文件的数据结构前，大家必须得了解Lucene的一个工具类[FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/FixedBitSet)，这个类在源码中有大量的应用，是必须熟悉的一个工具。
# 数据结构
图1：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/liv/1.png">
## CurrentBits
CurrentBits占固定8个字节，即写入的是一个long类型的值，每一个CurrentBits分别表示了[FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/FixedBitSet)对象中的bits[]数组的元素。
# 例子
图2：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/liv/2.png">
上图中添加了10篇文档，对应文档号0~9，然后在第84、85行执行了删除操作，即满足域名为"content"，域值为"h"或者"f"  的文档都会被删除，即文档号0、4、7会被删除。在删除操作以后，在查询阶段实际可以获得的文档号只有1、2、3、5、6、8、9。
由于一共只有10篇文档，所以只要一个long类型的值就可以表示这些文档号，即FixedBitSet对象中的long bit[]数组只有一个元素，数组下标代表了文档号。
图3：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/索引文件/liv/3.png">

# 结语
.liv索引文件非常的简单，只要熟悉[FixedBitSet](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0404/FixedBitSet)的用法，相信其数据结构也一目了然。

[点击下载](http://www.amazingkoala.com.cn/attachment/Lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/liv.zip)Markdown文件