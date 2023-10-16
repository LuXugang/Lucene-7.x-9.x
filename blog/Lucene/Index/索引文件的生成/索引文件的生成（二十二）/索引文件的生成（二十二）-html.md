---
title: 索引文件的生成（二十二）之nvd&&nvm（Lucene 8.4.0）
date: 2020-08-28 00:00:00
tags: [nvd,nvm]
categories:
- Lucene
- Index
---

&emsp;&emsp;在执行[flush()](https://www.amazingkoala.com.cn/Lucene/Index/2019/0716/文档提交之flush（一）)的过程中，Lucene会将内存中的索引信息生成索引文件，本篇文章继续介绍[索引文件.nvd&&.nvm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0305/索引文件之nvd&&nvm)，其生成的时机点如下图红色框标注：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十二）/1.png">

&emsp;&emsp;图1的流程图属于Lucene 7.5.0，在Lucene 8.4.0中同样适用，该流程图为flush()过程中的一个流程点，详情见文章[文档提交之flush（二）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0718/文档提交之flush（二）)。

&emsp;&emsp;生成索引文件.nvd&&.nvm的目的在于存储normValue值以及文档号，我们先了解下在索引阶段（index phase），Lucene是如何计算、收集normValue值的。

## 计算文档的normValue值

&emsp;&emsp;在文章[查询原理（四）](https://www.amazingkoala.com.cn/Lucene/Search/2019/0827/查询原理（四）)中介绍打分公式时我们知道，计算一篇文档的分数时会考虑一个norm值，该值描述的是文档长度对打分的影响，并且norm值是通过cache[ ]（数组长度为256）数组获得的，而normValue则是作为该数组的下标值。

&emsp;&emsp;normValue的全名为normalization value，标准化的值，通过Lucene中的[SmallFloat.intToByte4(int numTerms)](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-7.5.0/lucene/core/src/java/org/apache/lucene/util/SmallFloat.java)方法生成一个标准化的值，该方法的参数numTerms描述的是文档的长度，文档的长度的计算方式取决于不同的打分公式，我们以默认的打分公式BM25为例展开介绍：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十二）/2.png">

&emsp;&emsp;图2中先计算出numTerms，然后通过SmallFloat.intToByte4方法生成一个标准化的值，即normValue。

&emsp;&emsp;由图2可知numTerms共有三种计算方式：

- 119行代码：一篇文档中包含某个域的域值数量（去重，即重复的域值不纳入域值数量）
- 123行代码：一篇文档中包含某个域的域值数量（非去重）
- 121行代码：非去重的域值数量 与 state.getNumOverlap()的差值，该方法的含义在介绍分词会展开

&emsp;&emsp;注意的是在索引阶段，每处理一篇文档，会计算文档中每个域对应的文档长度，因为在查询阶段，无法知道会用哪个域作为查询条件。

&emsp;&emsp;**为什么要执行标准化的操作**

&emsp;&emsp;由numTerms的计算方式可以看出，如果直接采用numTerms，会造成突兀的域值数量对打分公式产生显著的影响，故需要通过SmallFloat.intToByte4方法平缓该影响，该方法的返回值为byte类型，类似归一化操作，该方法将文档长度标准化到[1, 255]的取值区间，即computerNorm的返回值的取值范围为[1, 255]，注意的是，normValue == 1时候为一个特殊值，它描述了当前域不考虑文档的长度，代码中，可以通过FieldType.setOmitNorms(true)方法设置。


## 收集文档的normValue值

&emsp;&emsp;同其他索引文件一样，每个域都会各自收集normValue值，对应在源码中，每个域都会对应生成一个[NormValuesWriter](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/index/NormValuesWriter.java)来实现收集。

&emsp;&emsp;收集的过程中主要收集文档号跟normValue，其中文档号使用[DocsWithFieldSet](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/index/DocsWithFieldSet.java)对象收集，该对象的处理逻辑见文章[索引文件的生成（十五）之dvm&&dvd](https://www.amazingkoala.com.cn/Lucene/Index/2020/0507/索引文件的生成（十五）之dvm&&dvd)，另外使用PackedLongValues对象收集normValue值，该对象的介绍见文章[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/PackedInts（一）)。

## 生成索引文件.nvd&&.nvm

&emsp;&emsp;由于之前介绍过了其他索引文件的生成过程，相比较下来索引文件.nvd&&.nvm的生成过程过于简单并且雷同，所以就不写了。。。

## 结语

&emsp;&emsp;对于该索引文件的内容在随后介绍索引文件.doc、pos、pay的读取的文章中会顺便提及。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的生成/索引文件的生成（二十二）/索引文件的生成（二十二）.zip)下载附件

