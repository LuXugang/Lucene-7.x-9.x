---
title: BulkScorer（二）（Lucene 9.6.0）
date: 2023-07-24 00:00:00
tags: [bulkScorer]
categories:
- Lucene
- Search
---

&emsp;&emsp;本篇文章我们继续介绍BulkScorer的其他子类，下图为BulkScorer主要的几个子类，其中`DefaultBulkScorer`的介绍可以见文章[BulkScorer（一）](https://www.amazingkoala.com.cn/Lucene/Search/2023/0707/BulkScorer（一）)：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/1.png"  width="700">

## ReqExclBulkScorer

### 实现逻辑

&emsp;&emsp;ReqExclBulkScorer中包含了两个成员，一个是名为`req`的BulkScorer对象，另一个是名为`excl`的[DocIdSetIterator](https://www.amazingkoala.com.cn/Lucene/Search/2023/0707/BulkScorer（一）)对象，它包含了在查询条件中指定的不需要返回的文档号集合（MUST_NOT）。

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/2.png">

&emsp;&emsp;在文章[BulkScorer（一）](https://www.amazingkoala.com.cn/Lucene/Search/2023/0707/BulkScorer（一）)中我们说到，BulkScorer的score()方法描述的是对某个文档号区间进行遍历，期间过滤掉被删除的文档号，最后使用[Collector](https://www.amazingkoala.com.cn/Lucene/Search/2019/0812/Collector（一）)收集文档号。在子类ReqExclBulkScorer的实现中，则是根据`excl`中的文档号将`req`的文档号集合划分为一个或多个更小的集合。

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/3.png">

&emsp;&emsp;图2中，excl中的文档号将req中待遍历的结合划分为3个区间。**在源码实现中，每一个区间对应一次BulkScorer的score()方法的调用**，差别在于不同的文档号区间范围。

### 使用场景

&emsp;&emsp;例如在使用BooleanQuery时，特定查询条件下会使用到ReqExclBulkScorer，其具体逻辑会在介绍BooleanQuery中获取BulkScorer的文章中展开介绍。

## TimeLimitingBulkScorer

&emsp;&emsp;TimeLimitingBulkScorer用于为BulkScorer设定一个超时时间，查询超时后通过抛出异常的方式结束BulkScorer的scorer方法的调用。

&emsp;&emsp;TimeLimitingBulkScorer封装了一个名为`in`的BulkScorer对象，允许用户设定一个名为`queryTimeout`的QueryTimeout对象作为查询超时条件。

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/4.png" width="1000">

&emsp;&emsp;QueryTimeout类很简单，类中就包含一个`shouldExit`的方法，下文中会介绍该方法在何时会被调用。

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/5.png" width="1000">

&emsp;&emsp;超时条件通过IndexSearcher类中的setTimeout()方法设定。

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/6.png" width="1000">

### 实现逻辑

&emsp;&emsp;TimeLimitingBulkScorer实现BulkScorer的score()的逻辑中，跟ReqExclBulkScorer相似的地方是将封装的BulkScorer对象（图3中的`in`）的遍历区间拆封成多个一个或多个区间，每遍历完一个区间就调用图4中的`shouldExit()`方法判断是否已经超时。区间的划分规则则是基于根据文档号数量，每个区间中的文档号数量为interval，其公式如下所示：

```java
int interval = LastInterval + (LastInterval >> 1)
```

&emsp;&emsp;LastInterval的值为上一个区间的interval，并且LastInterval的初始值为100。

- 第一个区间中的文档号数量：即初始值100
- 第二个区间中的文档号数量：(100 + 100 >>2) =  150
- 第三个区间中的文档号数量：(150 + 150 >> 2) = 225

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/BulkScorer/BulkScorer（二）/7.png">


#### 使用场景

&emsp;&emsp;当在IndexSearcher对象中指定了setTimeout()方法后，所有的BulkScorer对象都会进行超时判断。

## 结语


&emsp;&emsp;基于篇幅，剩余的内容将在下一篇文章中展开介绍。

[点击](https://www.amazingkoala.com.cn/attachment/Lucene/Search/BulkScorer/BulkScorer（二）.zip)下载附件