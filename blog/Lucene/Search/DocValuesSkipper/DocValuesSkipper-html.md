---
title: DocValuesSkipper（Lucene 10.0.0）
date: 2024-10-23 00:00:00
tags: [skipList, docValues, range,index]
categories:
- Lucene
- Search
---

从Lucene 10.0.0开始，新增了DocValuesSkipper功能，用来提高正排索引DocValues类型范围查询（[SortedNumericDocValuesRangeQuery](https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/document/SortedNumericDocValuesRangeQuery.java)、[SortedSetDocValuesRangeQuery](https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/document/SortedSetDocValuesRangeQuery.java)）的遍历文档号的能力。

提出该优化的issue见：[LUCENE-10396](https://github.com/apache/lucene/issues/11432)。

## 概述

我们以`SortedNumericDocValuesRangeQuery`的范围查询为例，比较优化前后遍历文档的差异：

### 未使用IndexSort

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/1.png"  width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/disableindexsort.html)

- 优化前：`SortedNumericDocValuesRangeQuery`通过逐个遍历文档，然后判断每一篇文档的DocValues值是否在`[50, 70]`中。

- 优化后：在索引期间，每处理N（源码中为`DEFAULT_SKIP_INDEX_INTERVAL_SIZE = 4096`）个DocValues生成一个`SkipAccumulator`的数据结构。为了便于描述，图1中每3篇文档就生一个`SkipAccumulator`。
  - 在查询期间，通过`SkipAccumulator`中的`minxValue`和`maxValue`与查询范围`[50, 70]`比较，不在范围内的`SkipAccumulator`则跳过，否则就逐个遍历`SkipAccumulator`中的文档，判断这些文档对应的DocValues值是否在`[50, 70]`中

### 使用IndexSort

#### 没有缺失值

根据DocValus排序的前提下，如果没有缺失值（每篇文档中都有DocValues），那么只需要根据Query Range的上下界找到对应的文档号即可。

意味着文档号按照DocValues排序：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/2.png"  width="">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/enableindexsort.html)

图1中，根据Query Range的上下界，分别找到对应的文档号`10`、`14`那么满足查询提交的文档号集合为[10, 14]。

#### 存在缺失值

该场景的处理方式跟图1一致。

## SkipIndex

上文中优化后的正排索引通过跳跃（Skip）实现了次线性时间复杂度的文档遍历。其原理依赖在索引期间构建的`SkipIndex`。我们先看下优化后的索引文件数据结构：

### dvd

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/3.png"  width="600">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/dvd.html)

图3中，除了`SkipIndexData`，其他字段已在文章[索引文件的生成（十七）之dvm&&dvd（Lucene 8.4.0）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0526/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6%E7%9A%84%E7%94%9F%E6%88%90%EF%BC%88%E5%8D%81%E4%B8%83%EF%BC%89%E4%B9%8Bdvm&&dvd/)中介绍。

图2中`SkipAccumulator`即索引文件中的`skipAccumulator`。

新增的`SkipIndexData`对应的这些字段，我们暂时先不介绍，等下文中介绍跳表（skipList）时一并介绍，更容易理解。

### dvm

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/4.png"  width="5800">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/dvm.html)

图4中除了`SkipIndexMetaData`字段，其他字段已在文章[索引文件的生成（十七）之dvm&&dvd（Lucene 8.4.0）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0526/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6%E7%9A%84%E7%94%9F%E6%88%90%EF%BC%88%E5%8D%81%E4%B8%83%EF%BC%89%E4%B9%8Bdvm&&dvd/)中介绍。

- offset、length：offset跟length用来描述在索引文件.dvd中`SkipIndexData`的位置区间

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/5.png"  width="1000">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/dvmtodvd.html)

- globalMaxValue：所有文档中最大的DocValues值

- globalMinValue：所有文档中最小的DocValues值

- globalDocCount：包含DocValues的文档数量（可以在读取阶段用来判断是否存在缺失值：文档中没有DocValues视为缺失）

- maxDocId：最大文档号

- ord：布尔类型的值（ord = 1：多值）

  - 某一篇文档中如果索引了某个域名的多个域值视为多值

  ```java
  Document doc = new Document();
  // 多值
  doc.add(new SortedNumericDocValuesField("content", 2));
  doc.add(new SortedNumericDocValuesField("content", 4));
  ```

## SkipList

`DocValuesSkipper`通过跳表（SkipList）实现文档号的跳转，然而SkipList是一个多层的数据结构。在索引期间，每处理N（源码中为`DEFAULT_SKIP_INDEX_INTERVAL_SIZE = 4096`）个DocValues生成一个`SkipAccumulator`的数据结构。这种方式生成的`SkipAccumulator`被定义为属于SkipList的最底层（level = 0）。

- 为了便于描述，文章中每处理`2`个DocValues后生成一个`SkipAccumulator`。

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/6.png"  width="">

随后每当生成`1 << SKIP_INDEX_LEVEL_SHIFT`（源码中`SKIP_INDEX_LEVEL_SHIFT`的值为`3`，那么`1 << 3 = 8`）个`SkipAccumulator`后，则在level = 1层生成一个新的`SkipAccumulator`，并且合并这8个`SkipAccumulator`中的信息：

- 为了便于描述，文章中每处理2个`SkipAccumulator`，则在新的层级中生成一个新的`SkipAccumulator`

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/7.png"  width="1000">

同样的在level = 1中，每处理2个`SkipAccumulator`就要再新的层级中生成一个新的`SkipAccumulator`，依次类似，最后的跳表如下所示：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/8.png"  width="1000">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/skiplist.html)

源码中，定义了参数`SKIP_INDEX_MAX_LEVEL`，限制SkipList的最大层数为`4`，并且要求每处理8个`SkipAccumulator`生成一个新的`SkipAccumulator`。因此在level = 0 层，最多有`512`个`SkipAccumulator`。

将图8中写入到索引文件.dvm中，其字段对应关系如下：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/9.png"  width="1000">

[查看大图](http://www.amazingkoala.com.cn/uploads/lucene/Search/DocValuesSkipper/relation.html)

## 结语

本篇文章简单介绍了DocValuesSkipper加强DocValues的范围查询遍历能力的机制，SkipList以及正排索引文件的变化。然而如何通过SkipList实现跳转并未体现在文中。DocValuesSkipper是一个刚刚上线的功能（2024年10月14日随着Lucene 10.0.0发布），相信该功能还会进一步优化迭代，作者准备等待后续几次新版本后再来介绍。