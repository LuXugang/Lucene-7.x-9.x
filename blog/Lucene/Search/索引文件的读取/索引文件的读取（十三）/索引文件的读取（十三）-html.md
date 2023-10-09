---
title: 索引文件的读取（十三）之doc&&pos&&pay（Lucene 8.4.0）
date: 2020-09-11 00:00:00
tags: [index,pos,doc,pay]
categories:
- Lucene
- Search
---

&emsp;&emsp;本文承接文章[索引文件的读取（十二）之doc&&pos&&pay](https://www.amazingkoala.com.cn/Lucene/Search/2020/0904/索引文件的读取（十二）之doc&&pos&&pay)，继续介绍剩余的内容。索引文件.doc、.pos、.pay的读取过程相比索引文件.tim&&.tip较为简单，核心部分为如何通过读取这三个索引文件，**生成一个PostingsEnum对象**，该对象中描述了term在一篇文档中的词频frequency、位置position、在文档中的[偏移offset](https://www.amazingkoala.com.cn/Lucene/Index/2019/0222/倒排表（上）)、[负载payload](https://github.com/luxugang/Lucene-7.5.0/blob/master/LuceneDemo/src/main/java/lucene/AnalyzerTest/PayloadAnalyzer.java)以及该文档的文档号docId，其中docId和frequency通过[索引文件.doc](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/索引文件之doc)获得、position通过[索引文件.pos](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/索引文件之pos&&pay)获得、offset和payload通过[索引文件.pay](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/索引文件之pos&&pay)获得。

## PostingsEnum

&emsp;&emsp;PostingsEnum是一个抽象类，其子类的实现有很多，本文中仅仅介绍在[Lucene84PostingsReader](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/codecs/lucene84/Lucene84PostingsReader.java)类中的子类，如下所示：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/1.png">

&emsp;&emsp;图1中共有5个子类实现，用<font color=red>红框</font>标注，在搜索阶段，通过下面两个条件来选择其中一种实现：

- 条件一：Flag
- 条件二：打分模式ScoreMode

### Flag

&emsp;&emsp;Flag描述了在搜索阶段中，我们需要获取term在文档中的哪些信息。这里的信息即上文中提到的frequency、position、offset以及payload。由于不是所有的查询都需要所有的这些信息，选择性（optional）的获取这些信息能降低搜索阶段的内存开销，同时减少读取索引文件时产生的磁盘I/O，下文中会详细介绍。

&emsp;&emsp;Flag的可选值如下所示：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/2.png">

### 打分模式ScoreMode

&emsp;&emsp;ScoreMode描述的是搜索模式，正如源码中的注释：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/3.png">

&emsp;&emsp;本文中我们不展开ScoreMode的详细介绍，我们仅仅看下图3中<font color=red>红框</font>标注的TOP_SCORES，该值影响了上文中PostingsEnum子类的选择，该注释源码大意为：在搜索阶段，不会匹配所有满足查询条件的文档，会跳过那些不具竞争力的文档。其原理就是利用了索引文件.doc中的[Impacts字段](https://www.amazingkoala.com.cn/Lucene/Search/2020/0904/索引文件的读取（十二）之doc&&pos&&pay)实现的，这里简单提下，在以后介绍[WAND（weak and）算法](https://issues.apache.org/jira/browse/LUCENE-4100?jql=text%20~%20%22WAND%22)时候再详细介绍。

### 选择PostingsEnum的实现类

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/4.png">

&emsp;&emsp;图4的流程图描述了如何根据Flag跟打分模式选择PostingsEnum的实现类。`是否有跳表信息`的判断依据为：如果包含term的文档数量小于128，即一个block的大小，那么在生成索引文件.doc阶段就不会有跳表信息（不懂？见文章[索引文件的生成（三）之跳表SkipList](https://www.amazingkoala.com.cn/Lucene/Index/2020/0103/索引文件的生成（三）之跳表SkipList)）;在读取阶段由于是先读取索引文件.tim，故通过该索引文件中的DocFreq字段来获取包含term的文档数量，如下图红框标注的字段：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/5.png">

&emsp;&emsp;图5索引文件.tim的字段介绍以及生成过程可以分别阅读文章[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/索引文件之tim&&tip)、[索引文件的读取（七）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Search/2020/0804/索引文件的读取（七）之tim&&tip)。

### PostingsEnum中的信息

&emsp;&emsp;我们先直接列出每个实现类中包含的数据，随后介绍获取这些信息的方式。

- BlockDocsEnum：
  - docId集合
  - frequency集合
- EverythingEnum：
  - docId集合
  - frequency集合
  - position集合
  - offset集合
  - payload集合
- BlockImpactsDocsEnum：
  - docId集合
  - frequency集合
  - impactData信息
- BlockImpactsPostingsEnum：
  - docId集合
  - frequency集合
  - position集合
  - impactData信息
- BlockImpactsEverythingEnum：
  - docId集合
  - frequency集合
  - position集合
  - offset集合
  - payload集合
  - impactData信息

&emsp;&emsp;无论哪一种PostingsEnum的实现类，在读取过程中，**每次总是从索引文件.doc、pos、pay只读取一个block的信息**，并把block中的信息写入到多个数组中（下文会介绍这些数组），这些数组用来描述上文中docId集合、frequency集合、position集合、offset集合、payload集合、impactData信息。

**如何读取一个block中的信息**

&emsp;&emsp;为了便于描述，我们不考虑**没有生成跳表**以及**不满128条信息的block（见文章[索引文件之doc](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/索引文件之doc)中VIntBlocks的介绍）**的读取，只介绍生成跳表后的读取方式。

&emsp;&emsp;在读取了跳表SkipList之后（读取过程见文章[索引文件的生成（四）之跳表SkipList](https://www.amazingkoala.com.cn/Lucene/Index/2020/0106/索引文件的生成（四）之跳表SkipList)），我们就获得了一个SkipDatum的信息。

&emsp;&emsp;这里需要简单说明下，在生成跳表SkipList的过程中，在第0层中每当处理skipInterval（默认值为128）篇文档就生成一个SkipDatum，另外每生成skipMultiplier（默认值为8）个SkipDatum就在上一层，即第1层，生成一个SkipDatum。注意的是第1层的该SkipDatum中包含的指针信息是指向第0层中最后一个SKipDatum的**结束读取位置**，同时意味着指针信息指向了第0层的最后一个SkipDatum的下一个**待写入**的SkipDatum的**起始读取位置**，如果不了解这段描述，请阅读文章[索引文件的生成（三）之跳表SkipList](https://www.amazingkoala.com.cn/Lucene/Index/2020/0103/索引文件的生成（三）之跳表SkipList)。

&emsp;&emsp;**那么此时问题来了，在读取阶段，最高层的第一个跳表是如何读取的；term的第一个docId信息、frequency信息、position信息、offset信息、payload信息是如何获得的呢**？

- 通过图5中索引文件.tim中的TermMetadata字段中的SkipOffset获得最高层的第一个跳表信息，通过DocStartFP、PosStartFP、PayStartFP获取term的第一个docId信息、frequency信息、position信息、offset信息、payload信息分别在索引文件.doc、.pos、.pay中的起始读取位置。

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/6.png">

&emsp;&emsp;在SkipDatum字段包含的信息中，DocFPSkip描述了存储docId跟frequency的block在索引文件.doc中的起始读取位置，PosFPSkip描述了存储position的block在索引文件.pos中的起始读取位置，PosBlockOffset描述了block中的块内偏移（不明白的话，请阅读文章[索引文件的生成（一）之doc&&pay&&pos](https://www.amazingkoala.com.cn/Lucene/Index/2019/1226/索引文件的生成（一）之doc&&pay&&pos)、[索引文件的生成（二）之doc&&pay&&pos](https://www.amazingkoala.com.cn/Lucene/Index/2019/1227/索引文件的生成（二）之doc&&pay&&pos)）剩余的字段同理，下文中会进一步介绍，最终读取后的信息写入到上文提到的各种集合中：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/7.png">

[点击](http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/point.html)查看大图

#### docId集合、frequency集合

&emsp;&emsp;在源码中，使用docBuffer[ ]、freqBuffer[ ]两个数组来描述在内存中一个block中的的docId、frequency集合信息，从下面的定义也可以看出这两个数组都只存储一个block大小的信息：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/8.png">

&emsp;&emsp;通过读取索引文件.doc的TermFreqs字段中的PackedDocDeltaBlock跟PackedFreqBlock字段，就可以获得docId集合跟frequency集合，并将这两个字段的信息分别写入到docBuffer[ ]、freqBuffer[ ]中：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/9.png">

&emsp;&emsp;这里要特别说明的是，在读取block时，总是会将PackedDocDeltaBlock的信息，即文档号信息，写入到docBuffer\[ ]中，而PackeddFreqBlock的信息，即frequency词频信息，则是采用**read lazily**，它描述的是在索引阶段存储了文档的frequency信息（基于[IndexOptions](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/index/IndexOptions.java)选项），但是在搜索阶段，某种查询并不需要frequency信息，那么只有在需要frequency信息时候才读取PackedFreqBlock，并且写入到freqBuffer[ ]中。

#### position集合、offset集合、payload集合

&emsp;&emsp;在源码中，使用posDeltaBuffer[ ]描述position信息；使用offsetStartDeltaBuffer[ ]、offsetLengthBuffer[ ]描述offset信息；使用payloadLengthBuffer[ ]、payloadBytes[ ]描述payload信息：

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/10.png">

&emsp;&emsp;对于这三个信息的读取相比较读取文档号跟词频稍微复杂，原因在于term在一篇文档中的这三个信息可能分布在一个或多个block（图7中的PackedPosBlock、packedPayBlock）中、甚至有可能在同一个PackedPosBlock中，保存term在两篇文档或更多篇文档的position信息，并且这些文档的属于不同的SkipDatum管理。例如term在一篇文档中的词频为386次，由于每128个位置信息就生成一个PackedPosBlock，故需要3个block存储。

&emsp;&emsp;对于上述的情况，我们以position为例，通过SkipDatum中的PosFPSkip先从索引文件.pos中找到block，即PackedPosBlock，的起始读取位置，然后将PosBlockOffset作为**块内偏移**找到在block中的起始读取位置即可。

&emsp;&emsp;由于position信息、offset信息、payload信息的数量总是保持一致的，即term在文档中的某个位置，必定对应有一个offset以及payload（可以为空），所以存储这些信息的posDeltaBuffer[ ]、offsetStartDeltaBuffer[ ]、offsetLengthBuffer[ ]、payloadLengthBuffer[ ]这四个数组的数组下标是保持一致的，注意的是payloadLengthBuffer[ ]描述的是在某个位置的term的payload长度length，根据这个长度去payloadBytes[ ]读取payload数据：

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十三）/11.png">

#### impactData信息

&emsp;&emsp;最后剩余读取impactData信息就相对简单了，在源码中，用二维数组impactData\[ ] \[ ]、impactDataLength\[ ]来描述所有层的Impacts信息（Impacts的概念见文章[索引文件的读取（十二）之doc&&pos&&pay](https://www.amazingkoala.com.cn/Lucene/Search/2020/0904/索引文件的读取（十二）之doc&&pos&&pay)）、图6中，先读取ImpactLength字段，确定Impacts字段的读取区间，然后将Impacts字段的信息写入到这两个数组中，这两个数组跟存储payload信息的两个数组用法一致，不赘述。

## 结语

&emsp;&emsp;关于索引文件.doc、.pos、.pay的一些基本的读取逻辑就暂时介绍到这里，在后面的文章介绍[WAND（weak and）算法](https://issues.apache.org/jira/browse/LUCENE-4100?jql=text%20~%20%22WAND%22)时，我们再进一步展开几个方法，例如advance( )、nextDoc( )、slowAdvance( )、advanceShallow( )等等，这些方法更细节的实现了读取索引文件.doc、.pos、.pay的过程。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/索引文件的读取（十三）/索引文件的读取（十三）.zip)下载附件



