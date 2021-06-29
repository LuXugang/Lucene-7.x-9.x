# [查询TopN的优化之NumericDocValues（二）](https://www.amazingkoala.com.cn/Lucene/Search/)（Lucene 8.9.0）

&emsp;&emsp;在上一篇[文章](https://www.amazingkoala.com.cn/Lucene/Search/2021/0621/193.html)的结尾，我们总结了使用NumericDocValues优化查询TopN的原理：假设查询TopN的排序规则为按照正排值从小大小的顺序，即正排值越小，优先级越高。故在开启优化后，当收集器收到一个文档号，先根据文档号从正排索引中拿到正排值，在**满足某些条件**后，根据正排值，通过查询BKD树获取所有小于该正排值的文档集合，该文档集合用于**生成一个新的[迭代器](https://www.amazingkoala.com.cn/Lucene/gongjulei/2021/0623/194.html)**。随后每次传入到收集器的文档号将会从新的迭代器中获取，达到所谓的skip non-competitive documents的效果。

&emsp;&emsp;上文的描述中，我们需要对两个问题进一步展开介绍：

- 问题一：满足什么条件后会生成一个新的迭代器
- 问题二：如何生成一个新的迭代器

## 问题一：满足什么条件后会生成一个新的迭代器

&emsp;&emsp;满足的条件很苛刻，本文中只挑选出部分条件，且这些条件必须同时满足。完整的内容可以阅读源码：NumericComparator类中的[updateCompetitiveIterator()](https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/search/comparators/NumericComparator.java)方法。

### 条件一：Collector已经收集了N个文档号

&emsp;&emsp;只有在收集器收集了N个文档号后才会考虑是否需要生成一个新的迭代器。对应代码中通过判断用于收集文档信息的优先级队列queue是否full来判断。


### 条件二：是否允许使用PointValues来优化

&emsp;&emsp;在上一[文章](https://www.amazingkoala.com.cn/Lucene/Search/2021/0621/193.html)中说过，开启优化的其中一点是需要显示指定开启优化。在显示指定后，对应代码中一个布尔类型的enableSkipping会被置为true。

### 条件三：Collector是否已经处理了totalHitsThreshold个文档号

&emsp;&emsp;totalHitsThreshold是创建实现TopN的Collector（[TopFieldCollector](https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/search/TopFieldCollector.java)类的create方法）时，允许用户指定的一个int类型参数。totalHitsThreshold描述的是Collector至少处理了totalHitsThreshold个文档号后才会开启优化。本文介绍的优化是其中一种。另外还有基于[IndexSort]()优化会在后续的文章中介绍。

&emsp;&emsp;在[Lucene 7.2.0](https://issues.apache.org/jira/browse/LUCENE-8059)之后，查询TopN首次引入了允许提前结束Collector收集文档号的优化（见文章[Collector（三）](https://www.amazingkoala.com.cn/Lucene/Search/2019/0814/84.html)），即在已经收集到了**全局**最具competitive的N个文档号，Collector不用再处理剩余的文档号。这个优化会导致一个用户体验的问题，有些用户使用的场景需要记录hit count ，即命中的文档数量（满足用户设置的查询条件的文档数量），提前退出会导致**没法将所有满足查询条件的文档号**传入到Collector，使得Collector中的totalHits（传入到Collector的文档号数量）的值总是小于等于hit count的，最终使得用户无法通过Collector获得精确的（accurate）hit count。

&emsp;&emsp;所以在这次优化中同时增加了一个用户可以配置的布尔参数trackTotalHits，如果参数为true，那么当Collector已经收集到了TopN的文档号，并且即使这N个文档号**已经是全局最具competitive的集合**，Collector仍然继续收集其他的文档号（只统计totalHits），最终使得totalHits的数量能等于hit count。

&emsp;&emsp;随后在[LUCENE-8060](https://issues.apache.org/jira/browse/LUCENE-8060)讨论下，最终在Lucene8.0.0之后，用int类型的参数totalHitsThreshold替换了trackTotalHits，使得既能让用户获得想要的hit count，又能在开启优化后，减少一定的Collector中处理的文档号数量。当totalHitsThreshold的值大于等于满足查询条件的文档数量时，其相当于trackTotalHits置为true。

### 条件四：是否超过迭代器的更新次数

&emsp;&emsp;在Collector收集文档号期间，当达到条件三或者达到条件一并且当前需要更新queue中堆顶元素时，Collector会尝试更新迭代器。每次尝试更新迭代器会使用一个int类型的updateCounter统计尝试更新的次数。如果满足下列的条件，那么不会生成一个新的迭代器：

```java
updateCounter > 256 && (updateCounter & 0x1f) != 0x1f
```

### 条件五：估算新的迭代器中的文档号数量是否低于阈值

&emsp;&emsp;在上文四个条件都满足的情况下，才需要考虑最后一个条件。从条件五中可知我们需要了解两个内容：如何估算新的迭代器中的文档号数量、如何设定阈值。

#### 如何估算新的迭代器中的文档号数量

&emsp;&emsp;如果当前的排序规则是从小到大的升序，那么条件一中提到的queue中的堆顶元素，即堆中竞争力最低的（weakest competitive ）的正排值，它就是堆中的最大值，我们称之为maxValueAsBytes。估算的逻辑为从BKD树中统计出比maxValueAsBytes小的正排值的数量estimatedNumberOfMatches，注意的是estimatedNumberOfMatches是一个估算值。

&emsp;&emsp;统计estimatedNumberOfMatches的逻辑就是深度遍历BKD树，其详细遍历过程见文章[索引文件的读取（一）之dim&&dii](https://www.amazingkoala.com.cn/Lucene/Search/2020/0427/135.html)的介绍，我们通过一个例子简单的概述下。

##### 例子

&emsp;&emsp;BKD树中存放了[1, 100]共100个正排值，其中maxValueAsBytes的值为60。

图1：

<img src="查询TopN的优化之NumericDocValues（二）-image/1.png">

###### 访问根节点

&emsp;&emsp;maxValueAsBytes与根节点的关系是CELL_CROSSES_QUERY（见文章[索引文件的读取（一）之dim&&dii](https://www.amazingkoala.com.cn/Lucene/Search/2020/0427/135.html)的介绍），那么依次访问根节点的左右子节点：节点一、节点八。

###### 访问节点一

&emsp;&emsp;由于maxValueAsBytes比节点一的最大值还要大，即maxValueAsBytes与节点一的关系是CELL_INSIDE_QUERY。此时可以累加计算estimatedNumberOfMatches的值，该值为节点三、节点四、节点六、节点七四个叶子节点中点数据的数量总和。在源码中，默认每个子节点中的点数据数量最大值为512，故计算方式为：512 * 叶子节点数量。

###### 访问节点八

&emsp;&emsp;由于节点一以及它所有子节点都处理结束，故下一个访问节点为节点八。

&emsp;&emsp;maxValueAsBytes与节点八的关系是CELL_CROSSES_QUERY，那么将依次访问节点八的左右子节点：节点九、节点十二。

###### 访问节点九

&emsp;&emsp;maxValueAsBytes与节点九的关系是CELL_CROSSES_QUERY，那么将以此访问节点十跟节点十一。

###### 访问节点十

&emsp;&emsp;maxValueAsBytes与节点十的关系是CELL_CROSSES_QUERY，注意的是由于节点十是叶子节点，在源码中，不会通过遍历叶子节点中的点数据来获得一个准确的estimatedNumberOfMatches，其计算方式为叶子节点中的默认点数据数量最大值的一半，即(512 + 1) / 2。

###### 访问节点十一、十二

&emsp;&emsp;maxValueAsBytes与节点十一、节点十二的关系是CELL_OUTSIDE_QUERY，即maxValueAsBytes比节点十一、节点十二的最小值还要小，故直接返回。

&emsp;&emsp;最终，累加在各个节点获得的estimatedNumberOfMatches作为新的迭代器中的文档号数量的估算值。

#### 如何设定阈值

&emsp;&emsp;阈值threshold的计算基于当前迭代器的开销值iteratorCost（见文章[迭代器](https://www.amazingkoala.com.cn/Lucene/gongjulei/2021/0623/194.html)中关于开销cost的介绍），如果获取了新的迭代器，那么iteratorCost会被更新为新的迭代器的开销值：

```java
    final long threshold = iteratorCost >>> 3;
```

&emsp;&emsp;如果estimatedNumberOfMatches的值大于等于，那么将不会更新迭代器。

## 问题二：如何生成一个新的迭代器

&emsp;&emsp;当问题一中所有条件都满足后，那么随后将根据maxValueAsBytes再次遍历BDK树，这次的遍历将精确的获取所有大于maxValueAsBytes的正排值对应的文档号。在遍历的过程中，使用[文档号收集器](https://www.amazingkoala.com.cn/Lucene/gongjulei/2021/0623/194.html)获取一个文档集合，并用这个集合生成一个新的迭代器。随后下一次传给Collector收集器的文档号将会从新的迭代器中获取。

## 一些其他细节

&emsp;&emsp;另外使用了一个int类型的maxDocVisited记录了Collector目前处理过的最大文档号，使得新的迭代器不会收集Collector已经处理过的文档号。

## 结语

&emsp;&emsp;无

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/查询TopN的优化之NumericDocValues/查询TopN的优化之NumericDocValues（二）.zip)下载附件

