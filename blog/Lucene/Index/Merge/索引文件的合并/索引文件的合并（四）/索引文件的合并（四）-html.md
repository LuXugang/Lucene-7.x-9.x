---
title: 索引文件的合并（四）之kdd&kdi&kdm（Lucene 8.7.0）
date: 2020-12-22 00:00:00
tags: [kdd,kdi,kdm]
categories:
- Lucene
- Index
---

&emsp;&emsp;本篇文章开始介绍[索引文件kdd&kdi&kdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1027/索引文件之kdd&kdi&kdm)的合并，由于**维度值为1**和**维度值大于等于2**的点数据对应的索引文件的合并方式有较大的差异，故我们分开介绍。本篇文章先对维度值为1的情况展开介绍，建议先阅读文章[索引文件的生成（二十五）之kdd&kdi&kdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1217/索引文件的生成（二十五）之kdd&kdi&kdm)，了解下维度值为1的点数据是如何生成索引文件的。

## 索引文件kdd&kdi&kdm的合并流程图（维度值为1）

图1：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/1.png">

&emsp;&emsp;图1中，流程点`构建BKD树的节点元数据（node metadata）`跟`生成索引文件.dim的元数据`跟[索引文件的生成（九）之dim&&dii](https://www.amazingkoala.com.cn/Lucene/Index/2020/0406/索引文件的生成（九）之dim&&dii)是一模一样的，唯一的差异在`构建BKD树的节点值（node value）`中，我们看下该流程点是如何运作的。

### 构建BKD树的节点值（node value）流程图 维度值等于1

图2：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/2.png">

&emsp;&emsp;图2中我们分别给出了维度为1的点数据在执行索引文件的合并以及索引文件的生成中对应的流程点`构建BKD树的节点值（node value）`，可以看出，<font color=red>红框</font>标注的流程点是一样，见文章[索引文件的生成（二十五）之kdd&kdi&kdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1217/索引文件的生成（二十五）之kdd&kdi&kdm)的介绍，不赘述。在`索引文件的生成`中，流程点`节点内的点数据排序`执行结束后，就获得了有序的点数据集合，随后**有序**的读取每一个点数据来执行剩余的流程点；在`索引文件的合并`中，则是通过一个优先级队列，使得可以从所有的待合并的索引文件中获取**有序**的点数据，随后执行剩余的流程点。

#### 待合并集合

图3：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/3.png">

&emsp;&emsp;待合并集合即MergeState对象中用于描述**所有段**的点数据信息的PointsReader集合，该集合中每一个reader描述了一个段中索引文件kdd&kdi&kdm的信息。MergeState对象的详细介绍见文章[索引文件的合并（一）之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1130/索引文件的合并（一）之fdx&&fdt&&fdm)。

#### 初始化优先级队列

图4：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/4.png">

&emsp;&emsp;使用优先级队列的目的是为了能从多个待合的段中按照字典序有序的获得点数据，因为bkd树中每个叶子节点中的点数据是有序的，并且对于某个叶子节点，它包含的最小的点数据肯定是大于等于左边的叶子节点中的最大的点数据，并且小于等于右边的叶子节点中的最小的点数据。从优先级队列中读取出的点数据可以看成生成一个有序的集合，使得可以实现**按块划分**（见文章[索引文件的生成（二十五）之kdd&kdi&kdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1217/索引文件的生成（二十五）之kdd&kdi&kdm)的介绍）。优先级队列的排序规则为点数据的值的字典序。

图5：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/5.png">

#### 优先级队列中的元素

&emsp;&emsp;在源码中，这个优先级队列叫做BKDMergeQueue，该优先级队列中的元素是MergeReader对象。我们介绍下MergeReader对象中一些重要的成员：

图6：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/6.png">

##### BKDReader.IntersectState

&emsp;&emsp;在初始化优先级队列BKDMergeQueue期间，对于某个段来说，会将**第一个**叶子节点中的所有点数据的文档号写入到BKDReader.IntersectState对象中、点数据的值写入到字节数组packedValues[ \]中。随后在优先级队列出堆的过程中，每次取一个文档号，以及对应的点数据值。

图7：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的合并/索引文件的合并（四）/7.png">

&emsp;&emsp;在文章[索引文件的生成（八）之dim&&dii](https://www.amazingkoala.com.cn/Lucene/Index/2020/0329/索引文件的生成（八）之dim&&dii)中我们说到，数值类型会被转为字节类型，故图6中，4个字节来表示一个数值（int类型）。由于每个数组占用的字节数量是固定，即4个字节，那么在读取packedValues[ \]时，可以随机读取第n个点数据的值。

&emsp;&emsp;另外文档号的集合使用int类型的数组存储。

&emsp;&emsp;注意的是，在当前阶段获得的文档号集合中，可能有些文档已经被标记为删除的，即图6中的packedValues[ \]数组中有些点数据值是不能被合并到新的段的。故在处理过程中，每次处理一篇文档时，会利用图5中的MergeState.DocMap来实现过滤。在初始化MergeState对象时，通过读取每个段中的[索引文件.liv](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0425/索引文件之liv)来初始化DocMap对象。DocMap对象还有一个作用就是用来实现待合并的段中的文档号与新段中的文档号的映射关系，该功能见文章[索引文件的合并（二）之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/Index/2020/1202/索引文件的合并（二）之fdx&&fdt&&fdm)的介绍。

#### 剩余流程点

&emsp;&emsp;在初始化优先级队列BKDMergeQueue之后，通过出堆操作，就可以依次获得有序的点数据。对于某个待合并的段来说，当叶子节点所有点数据都处理结束后，会读取下一个叶子节点的信息，即重新初始化图6中MergeReader中的信息，直到该段中所有叶子节点的点数据被处理结束。在剩余的流程点中，它们的处理方式跟维度值为1的索引文件的[生成过程](https://www.amazingkoala.com.cn/Lucene/Index/2020/1217/索引文件的生成（二十五）之kdd&kdi&kdm)是一模一样的，故不赘述。

## 结语

&emsp;&emsp;无

[点击](https://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的合并/索引文件的合并（四）/索引文件的合并（四）.zip)下载附件





