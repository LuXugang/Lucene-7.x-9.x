---
title: 倒排表（中）
date: 2019-04-28 00:00:00
tags: [inverted index,posting]
categories:
- Lucene
- Index
---

本篇文章介绍使用了词向量（TermVector）后的域生成的倒排表，在索引阶段，索引选项（indexOptions）不为NONE的域会生成一种[倒排表（上）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0222/倒排表（上）)，这种倒排表的特点是所有文档的所有域名的倒排表都会写在同一张中，后续会读取倒排表来生成[.doc](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/索引文件之doc
)、[.pos&&.pay](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/索引文件之pos&&pay)、[.tim&&.tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/索引文件之tim&&tip)、[.fdx&&.fdx](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0301/索引文件之fdx&&fdt)、[.nvd&&.nvm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0305/索引文件之nvd&&nvm)等索引文件。而本文章中设置了TermVector的域会生成另外一张倒排表，并且一篇文档中生成单独的倒排表，同文档中的所有域名的倒排表写在同一张中，并且后续生成[.tvd、.tvx](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0429/索引文件之tvx&&tvd)文件。
尽管有两类倒排表，但是实现逻辑是类似的，一些预备知识，下面例子中出现的各种数组、文档号&&词频组合存储、position&&payload 组合存储、倒排表存储空间分配跟扩容等概念在[倒排表（上）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0222/倒排表（上）)中已经介绍了，这里不赘述。

# 两种倒排表的区别与联系
索引选项（indexOptions）不为NONE的域.YES生成的倒排表数据结构如下：
图1：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/1.png">
TermVector生成的倒排表数据结构如下：
图2：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/2.png">

## 区别与联系
- 两种倒排表都记录了position、payload、offset信息
- 倒排表生成的逻辑中，先生成图1的倒排表再生成图2的倒排表，所以term的信息只需要存储一次来尽可能降低内存的使用，而TermVector生成的倒排表获得term信息的通过索引选项（indexOptions）不为NONE的域生成的倒排表信息获得。
# 例子
图3：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/3.png">
上图说明：

- 代码第49行表示倒排表中会存放 文档号、词频、位置、偏移信息
- 代码第52行表示我们为域名为"content"的域值分词后"book"添加一个值为"it is payload"的 payload值，而域名为"title"的中的"book"以及其他term都没有payload信息
- 通过自定义分词器来实现payload，当前例子中的分词器代码可以看[PayloadAnalyzer](https://github.com/luxugang/Lucene-7.5.0/blob/master/LuceneDemo/src/main/java/lucene/AnalyzerTest/PayloadAnalyzer.java)

**上文中提到每篇文档都会生成一个独立的倒排表，所以我们只介绍文档0中的域生成倒排表的过程。**
图4：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/4.png">

## 写入过程
### 处理文档0
#### 处理域名“content”
例子中使用的是自定义分词器[PayloadAnalyzer](https://github.com/luxugang/Lucene-7.5.0/blob/master/LuceneDemo/src/main/java/lucene/AnalyzerTest/PayloadAnalyzer.java),所以对于域名“content”来说，我们需要处理 "the"、“book”、“is”、"book"共四个term。

##### 处理 “the”
图5：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/5.png">

###### ByteBlockPool对象的buffers数组
- 下标值0："the"在文档0中的位置，即0，由于没有payload信息，组合存储后，位置值为（0 << 1 | 0）,即0
- 下标值5~6："the"在文档0中的偏移位置以及term的长度
###### IntBlockPool对象的buffers数组
- 下标值0：包含"the"的位置position信息、payload在ByteBlockPool对象的buffers数组写入的起始位置**(下文不赘述这个数组的更新)**
- 下标值1：下一次遇到"the"时，它的offset信息在ByteBlockPool对象的buffers数组写入的起始位置**(下文不赘述这个数组的更新)**
###### lastPositions[]数组
- lastPositions[]数组下标值为"the"的termId(0)的数组元素更新为0
###### freq[]数组
- freq[]数组下标值为"the"的termId(0)的数组元素更新为1
###### lastOffsets[]数组
- lastOffsets[]数组下标值为"the"的termId(0)的数组元素更新为3，目的是为了差值存储

##### 处理 “book”
图6：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/6.png">
###### ByteBlockPool对象的buffers数组
- 下标值10：第一个"book"在文档0中的位置，即1，由于带有payload信息，组合存储后，位置值为（1 << 1 | 1）,即3
- 下标值11~14：下标值值11~13 无法完全存储"book"在当前文档0中的payload的信息，故需要扩容，获得的新的分片的范围为下标值20~33的区间，故下标值11~14的4个字节成为一个索引(index)，并且索引值为20。
- 下标值30~31：下标值30~32 无法完全存储"book"在当前文档0中的payload的信息，故需要扩容，获得的新的分片的范围为下标值34~53的区间，故下标值30~33的4个字节成为一个索引(index)，并且索引值为34。
- 所以处理第一个"book"扩容了两次
- 下标值15~16：第一个"book"在文档0中的偏移位置以及term的长度
- 下标值20~29、34~37：payload的长度以及对应的ASCII

###### textStarts[] 数组
数组元素值14作为 索引选项（indexOptions）不为NONE的域生成的倒排表中的ByteBlockPool对象的buffers数组的下标，就可以获得"book"的term信息
##### 处理 "is"
图7：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/7.png">
###### ByteBlockPool对象的buffers数组
- 下标值54："is"在文档0中的位置，即2，由于没有payload信息，组合存储后，位置值为（2 << 1 | 0）,即4
- 下标值59~60："is"在文档0中的偏移位置以及term的长度
##### 处理 "book"
图8：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/8.png">

###### ByteBlockPool对象的buffers数组
- 下标值17~18：第二个"book"在文档0中的偏移位置以及term的长度
- 下标值38：第二个"book"在文档0中的位置，即3，根据图2获得上一个"book"在文档0中的位置是1，所以差值就是 （3 - 1）= 2，由于带有payload信息，组合存储后，位置值为（2 << 1 | 1）,即5
- 下标值39~52：payload的长度以及对应的ASCII
#### 处理域名“content”
##### 处理 “book”
图9：
<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/TermVector倒排表/9.png">
- 下标值64："title"域的"book"在文档0中的位置，即0，由于没有payload信息，位置值为（0 << 1 | 0）,即0
 - 下标值69~70："title"域的"book"在文档0中的偏移位置以及term的长度

# 结语
本篇文章介绍了如何构建TermVector生成的倒排表，在后面的文章中还会再介绍一个倒排表，即MemoryIndex中的倒排表

[点击下载](http://www.amazingkoala.com.cn/attachment/Lucene/Index/TermVector%E5%80%92%E6%8E%92%E8%A1%A8/TermVector.zip)Markdown文件