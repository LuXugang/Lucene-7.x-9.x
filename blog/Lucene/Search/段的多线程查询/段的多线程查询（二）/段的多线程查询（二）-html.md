---
title: 段的多线程查询（二）（Lucene 10.0.0）
date: 2024-10-31 00:00:00
tags: [search, multiThread, intra-segment, slice]
categories:
- Lucene
- Search
---

众所周知Lucene的索引数据由一个或多个段组成，并且通过下列方式定义一个`IndexSearcher`后就可以实现段的多线程查询：

```java
public IndexSearcher(IndexReaderContext context, Executor executor)
{ 
  ... ...
}
```

在Lucene 10.0.0之前，Lucene首先依据[划分规则](https://www.amazingkoala.com.cn/Lucene/Search/2023/0626/段的多线程查询（一）/#例子)将段分配到不同的slice中，每个slice由一个线程负责查询，该线程依次对其包含的段进行循环查询（参见图1中的slice）。

虽然多线程能够加速所有段的查询，但它无法解决单个大段（single big segment）带来的性能瓶颈问题（详见[LUCENE-8675](https://github.com/apache/lucene/issues/9721)）。该issue中提到的一个解决方案是引入段内并发查询（intra-segment search concurrency）。

最终在Lucene 10.0.0中实现了该方案。简而言之，该方案将单个大段拆分为多个小段，每个小段作为一个独立的slice，其余段则依照原有的方式进行分配。

## Slice

我们可以通过一个例子来说明优化前后的分配方式。假设我们有以下几个段及其包含的文档数量：

| 段的编号 | 文档数量 |
| :------: | :------: |
|   段0    | 170_000  |
|   段1    | 2000_000 |
|   段2    |  30_000  |
|   段3    | 180_000  |


图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/1.png"   width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/slice.html)

单个大段允许拆分为最多5个小段。

## 拆分文档集合

单个大段拆分为多个小段的过程实际上是一种逻辑拆分，它不会真正改变底层的物理数据结构。在 Lucene 中，描述单个段的数据结构是 `LeafReaderContext`。在优化后的方案中，`LeafReaderContext`被逻辑拆分为多个名为 `LeafReaderContextPartition` 的数据结构。

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/2.png"   width="">

对于未被拆分的段，也会用`LeafReaderContextPartition`进行包装，保持一致性便于后续查询逻辑。

以图1中的`段2`为例：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/3.png"   width="">

换言之，尽管5个线程在查询图2中这5个 `LeafReaderContextPartition` 的数据时，从物理上仍在访问`段1`，但它们各自处理不同的文档编号区间。

## 适配变更

实现了段内并发查询的功能后，需要对Lucene一些现有的其他功能进行适配才能保证查询正常工作。其中一个需要适配的就是`TotalHitCountCollector`。

### TotalHitCountCollector

在文章[Count（Lucene 9.11.0）](https://www.amazingkoala.com.cn/Lucene/Search/2024/1010/Count/)中详细介绍了`TotalHitCountCollector`，最好先看下这篇文章，因为一些概念不会在本文中展开介绍。

`TotalHitCountCollector`的功能是用来统计某个Query对应命中文档的数量`count`。它提供了两种方式进行统计：

- 方式一：基于[Weight#count](https://www.amazingkoala.com.cn/Lucene/Search/2024/1010/Count/#次线性时间复杂度的query)，以次线性时间复杂度（sub-linear）统计
- 方式二：基于收集器[Collector](https://www.amazingkoala.com.cn/Lucene/Search/2019/0812/Collector（一）/)，以线性时间复杂度统计，`count`响应时间与文档数量成正比

我们先看下基于方式二，统计文档的数量`count`是如何实现的：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/4.png"   width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/collector.html)

对于方式二，我们不需要适配变更，被拆分后的段之间是相互独立工作的，比如在`段1（partition-2）`中只会统计文档号集合[400_000, 800_000]中满足查询条件的`count`，即`count2`，因此只需要在最后通过`Reduce`将这些段的命中数量统计相加就可以得到在`段1`中的`count`值，我们称之为`countSum`。

然而基于方式一时，如果还是按照Lucene10.0.0之前的逻辑就会出现问题，因为在任意一个小段中通过`Weight#count`获取到是`段1`的`count`值`countSum`，就会出现下面的错误：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/5.png"   width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/collectorerror.html)

也就说如果采用方式一，我们只需要任意一个线程计算countSum就可以了，并且该结果可以让其他线程感知，当获知其他线程已经计算出`countSum`，那么自身就不再计算，直接退出即可。

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/6.png"   width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/collectorright.html)

图6中，`段1（partition-1）`成功抢占临界区，然后通过`Weight#count`获取`段1`中满足查询条件的`count`。

我们将图4跟图6的逻辑合并下就是适配变更后的新逻辑：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/7.png"   width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/段的多线程查询/段的多线程查询（二）/change.html)

## 改进

当前版本默认不启用段内并发查询，因为有些Query比如`PointRangeQuery`会有性能损失。其原因是这类Query在查询前会提前计算一些内容，开启段内并发查询会重复这些计算，当解决这些问题后才会开启段内并发查询，见[GITHUB-13745](https://github.com/apache/lucene/issues/13745)。
