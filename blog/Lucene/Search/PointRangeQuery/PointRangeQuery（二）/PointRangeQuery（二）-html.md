---
title: PointRangeQuery（二）（Lucene 8.11.0）
date: 2021-11-28 00:00:00
tags: [query,rangeQuery,point,dim,dii]
categories:
- Lucene
- Search
---

&emsp;&emsp;本文承接[PointRangeQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search/2021/1122/PointRangeQuery（一）)，继续介绍数值类型的范围查询PointRangeQuery。

## 节点访问规则IntersectVisitor

&emsp;&emsp;上一篇文章中我们说到，在收集文档号的策略中，除了策略一，不管哪一种策略，他们的**相同点都是使用深度遍历读取BKD树，不同点则是访问内部节点跟叶子节点的处理规则，这个规则即IntersectVisitor**。

&emsp;&emsp;IntersectVisitor在源码中是一个接口类，我们通过介绍它提供的方法来了解：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/PointRangeQuery/PointRangeQuery（二）/1.png">

### 访问内部节点

&emsp;&emsp;正如图1中compare(..)方法中的注释说到，这个方法用来判断查询条件跟当前访问的内部节点之间的Relation（见[PointRangeQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search/2021/1122/PointRangeQuery（一）)的介绍），决策出如何进一步处理该内部节点的子节点。我们先介绍下PointRangeQuery中如何实现**compare(...)**，随后在**访问叶子节点**时小结中介绍如何根据Relation作出访问子节点的策略。

#### PointRangeQuery中计算Relation的实现

&emsp;&emsp;其实现过程用一句话描述为：先判断是否为CELL_OUTSIDE_QUERY，如果不是再判断是CELL_CROSSES_QUERY还是CELL_INSIDE_QUERY。

##### 是否为CELL_OUTSIDE_QUERY

&emsp;&emsp;实现逻辑：依次处理每个维度，只要存在一个维度，查询条件在这个维度下的最小值比索引中的点数据在这个维度下的最大值还要大，或者查询条件在这个维度下的最大值比索引中的点数据在这个维度下的最小值还要小，那么它们的关系为CELL_OUTSIDE_QUERY。

&emsp;&emsp;我们以二维的点数据为例，并且我们称第一个维度为X维度，第二个维度为Y维度：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/PointRangeQuery/PointRangeQuery（二）/2.png">

&emsp;&emsp;图2中，在Y维度下，查询条件在Y维度下的最大值（1）比索引中的点数据在Y维度下的最小值（2）还要小，所以他们的关系是CELL_OUTSIDE_QUERY。

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/PointRangeQuery/PointRangeQuery（二）/3.png">

&emsp;&emsp;图3中，在Y维度下，查询条件在Y维度下的最小值（8）比索引中的点数据在Y维度下的最大值（6）还要大，所以他们的关系是CELL_OUTSIDE_QUERY。

##### CELL_CROSSES_QUERY还是CELL_INSIDE_QUERY

&emsp;&emsp;实现逻辑：**在不是CELL_OUTSIDE_QUERY的前提下**，只要存在一个维度，索引中的点数据在这个维度下的最小值比查询条件在这个维度下的最小值还要小，或者索引中的点数据在这个维度下的最大值比查询条件在这个维度下的最大值还要大，那么它们的关系为CELL_CROSSES_QUERY。如果所有维度**都不**满足上述条件，那么他们的关系为CELL_INSIDE_QUERY。

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/PointRangeQuery/PointRangeQuery（二）/4.png">

&emsp;&emsp;图4中，在不是CELL_OUTSIDE_QUERY的前提下，查询条件在Y维度下的最小值不会大于6, 那么因为索引中的点数据在Y维度下的最小值（2）比查询条件在这个维度下的最小值（4）还要小，那么他们的关系为CELL_CROSSES_QUERY。

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/PointRangeQuery/PointRangeQuery（二）/5.png">

&emsp;&emsp;图5中，在不是CELL_OUTSIDE_QUERY的前提下，不管是哪一个维度，都不满足索引中的点数据在这个维度下的最小值比查询条件在这个维度下的最小值还要小，也都不满足索引中的点数据在这个维度下的最大值比查询条件在这个维度下的最大值还要大，所以他们的关系为CELL_INSIDE_QUERY。

#### compare(...)方法的实现

&emsp;&emsp;在文章[PointRangeQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search/2021/1122/PointRangeQuery（一）)中说到，收集文档号集合有不同的策略，对于策略一由于不用遍历BKD树，所以不需要实现这个方法。而策略二跟策略三对compare(...)方法的实现有着少些的区别。

##### 策略三

&emsp;&emsp;该策略的对应的实现逻辑即上文中**PointRangeQuery中计算Relation的实现**介绍的内容。源码中的详细实现见类PointRangeQuery#getIntersectVisitor中的compare(...)方法。

##### 策略二

&emsp;&emsp;该策略首先采用同策略三一样的方式判断出查询条件跟内部节点的Relation，由于它采用反向收集文档号，所以它对应的实现也是"反向Relation"，由于该实现代码量较小，我们直接贴出源码：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/PointRangeQuery/PointRangeQuery（二）/6.png">

&emsp;&emsp;从图6可以看出，当代码225行计算出Relation的值为CELL_INSIDE_QUERY时，其"反向Relation"的值为CELL_OUTSIDE_QUERY，就如代码228行注释说的那样，如果内部节点下的所有子节点中的点数据都满足查询条件的话，那么就不用处理该内部节点的子节点，即不用再继续深度遍历该内部节点。同样的，当代码230行计算出Relation的值为CELL_OUTSIDE_QUERY，其"反向Relation"的值为CELL_INSIDE_QUERY。其代码231行注释中的"clear all documents"说的是在访问叶子节点的处理方式，我们在随后下一篇中会介绍。源码中的详细实现见类PointRangeQuery#getInverseIntersectVisitor中的compare(...)方法。

## 结语

&emsp;&emsp;基于篇幅，剩余内容将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/PointRangeQuery/PointRangeQuery（一）.zip)下载附件

