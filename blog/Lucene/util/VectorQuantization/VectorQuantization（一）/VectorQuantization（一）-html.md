---
title: Vector Quantization（一）（Lucene 9.10.0）
date: 2024-05-13 00:00:00
tags: [sq,scalar,quantization,vector,hnsw]
categories:
- Lucene
- gongjulei
---


本篇文章介绍下截止到9.10.0版本，Lucene中向量量化（Vector Quantization）技术相关的内容。

## Lucene中为什么要引入VQ

Lucene中使用[HNSW](https://amazingkoala.com.cn/Lucene/Index/2024/0118/HNSW%E5%9B%BE%E7%9A%84%E6%9E%84%E5%BB%BA/) (Hierarchical Navigable Small World) 实现了高维数据的搜索，引入VQ基于以下几个方面的考虑：

- **实际数据的需求**：对于给定的一个数据集，embeddings的每一个维度并不真正需要所有可能的亿级别的可选数值，这意味着每个维度的实际变化范围远小于float32类型所能提供的范围。
- **数据的高保真（fidelity）和浪费问题**：尽管浮点数类型可以提供最高的数据保真度（即数据的准确和细腻度），但在许多实际应用中，这种高保真度是过剩的。这是因为嵌入向量中真正重要的信息通常并不需要如此高的精度和如此多的数值选项。

当然这通常是以“有损”的方式进行的，意味着在处理过程中会丢失一些原始数据信息，但与此同时，它能显著减少存储数据所需的空间。

## 实现原理

VQ的实现方式在源码中核心代码为一个名为**ScalarQuantizer**类，它提供了以下两个关键的功能：float32->int8、校正偏移（Corrective Offset）。

### float32->int8

float32->int8描述的是将32位的浮点型数值用一个8位的整数表示，即使用[0, 127]的整数来代替浮点数，最终用`byte`类型存储。其量化公式也不是很复杂，由于在flush阶段已知了向量数据集中的最大跟最小值，因此使用了[min-max normalization ](https://en.wikipedia.org/wiki/Feature_scaling#Rescaling_(min-max_normalization))方法。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/1.png">

#### 向量距离

我们进一步看下，当使用了上文的量化公式后，在计算向量距离时有什么新的发现。

下图为两个float32向量中某一个维度的两个浮点数的乘积：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/2.png">

如果我们使用`α`表示`(max - min) / 127`，那么上述算式就变为：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/3.png">

以点积（[dot_product](https://en.wikipedia.org/wiki/Dot_product)）为例，如果向量的维度为`dim`，那么向量距离就是这`dim`个浮点数的乘积的和值，如下所示：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/4.png">

从图4可以看出，两个量化后的float32向量除了`dotProduct(int8, int8')`部分，其他部分都可以提前计算，可以直接存放在索引中，或者在查询期间只计算一次（见[qdrant](https://qdrant.tech/articles/scalar-quantization/)中的介绍）。 然而在Lucene的实现中，我们只需要计算量化后的向量距离，而图4中其他部分对于打分的影响则是通过校正偏移来实现。

### 校正偏移（Corrective Offset）

由于图4中只计算了量化后的距离，因此相比较量化前的距离，是存在精度损失的。校正偏移用于调整最终的文档打分（注意的是，在遍历图中节点时，两个节点的距离计算只使用点积或其他相关度算法，不需要考虑校正偏移，另外简单提下，最终的文档打分也需要考虑查询向量在量化后的校正偏移值），在对向量进行量化期间实现，向量的每个维度在float32转化为int8时都会产生校正偏移值，并且这个向量总的校正偏移值是每个维度的校正偏移值总和。

校正偏移的定义不在本文中展开，可以见该部分源码[作者](https://www.elastic.co/search-labs/author/benjamin-trent)的这几篇文章的介绍：[Scalar Quantization Optimized for Vector Databases](https://www.elastic.co/search-labs/blog/vector-db-optimized-scalar-quantization)。

下图描述的是**量化向量**跟**校正偏移**在[索引文件](https://amazingkoala.com.cn/Lucene/suoyinwenjian/2023/1225/vec&vem&vemf&vemq&veq&vex/)中的位置：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/5.png">

## 结语

截止到2024年5月31日，Lucene的量化技术进一步实现了float32->int4（还未正式发布），即32位的浮点数量化为4位的整数，整数范围为[0~15]。其量化过程跟float32->int4是一致的，由于使用byte数组存储，因此对于int4（即一个字节可以存放两个int4）还可以进一步压缩存储，如下图所以：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/6.png">

实现代码很简单，因此直接贴出：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/VectorQuantization/VectorQuantization（一）/7.png">
