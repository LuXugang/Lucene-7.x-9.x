---
title: DisjunctionMaxQuery（Lucene 8.9.0）
date: 2021-08-04 00:00:00
tags: [DisjunctionMaxQuery,query]
categories:
- Lucene
- Search
---

&emsp;&emsp;本系列的内容将会先介绍DisjunctionMaxQuery在Lucene中的实现原理，随后再介绍在Elasticsearch中的应用。我们先直接通过图1中DisjunctionMaxQuery的注释跟图2的构造函数来简单了解下这个Query的功能：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/1.png">

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/2.png">

&emsp;&emsp;<font color=gray>灰色框</font>标注的注释中说道：DisjunctionMaxQuery中封装了多个子Query（subqueries，这些子Query之间的关系相当于**BooleanQuery中SHOULD的关系**），即图2中的参数disjuncts。每个子Query都会生成一个文档集合，并且如果某篇文档被多个子Query命中，虽然这篇文档的打分值在不同的子Query中是不同的，但是这篇文档的**最终打分值**（在收集器Collector中的打分值）会选择分数最高的。不过如果图2中构造函数的参数tieBreakerMultiplier是一个不为0的合法值（合法取值范围为[0, 1)），那么文档的打分值还会所有考虑命中这个文档的所有子Query对应的打分值，其计算公式如下：

```java
        final float score = (float) (max + otherSum * tieBreakerMultiplier);
```

&emsp;&emsp;上述公式中，max为子Query对应最高的那个打分值，而otherSum则为剩余的子Query对应打分值的和值，由于tieBreakerMultiplier的取值范围为[0, 1]，所以当tieBreakerMultiplier==0时，文档的最终打分值为max；而当tieBreakerMultiplier==1时，则是所有子Query对应的打分值的和值，这个时候DisjunctionMaxQuery就相当于minimumNumberShouldMatch==1的BooleanQuery。在源码中，如果tieBreakerMultiplier==1，那么将会使用BooleanQuery进行查询。如下所示：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/3.png">

&emsp;&emsp;图3中，当tieBreakerMultiplier==1，会从disjuncts中读取所有的子Query，用于生成一个新的BooleanQuery，并且子Query之间的关系为should。

&emsp;&emsp;使用DisjunctionMaxQuery很有帮助的（useful）一个场景是：多个域中都包含某个term时，我们可以对不同的域设置加分因子（boost factor），如果某篇文档中包含这些域，我们获得的文档打分值**可以只是**加分因子最高的那个域对应的打分值，而不像在BooleanQuery中，文档打分值是所有域对应的打分值的和值。在随后介绍在Elasticsearch的应用中再详细展开。

## 例子

&emsp;&emsp;我们用一个例子来理解下图1中<font color=gray>灰色框</font>标注的注释：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/4.png">

### BooleanQuery（minimumNumberShouldMatch == 1）

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/5.png">

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/6.png">

&emsp;&emsp;图5中，文档0跟文档1中由于只包含了"title"跟"body"域的一个，并且查询条件BooleanQuery的minimumNumberShouldMatch的值为1，那么文档0跟文档1**分别满足**titleTermQuery跟bodyTermQuery，文档分数分别为0.113950975跟0.082873434。由于文档2**同时满足**了titleTermQuery跟bodyTermQuery这两个子Query ，所以文档2的分数为0.113950975跟0.082873434的和值，即0.1968244。

&emsp;&emsp;正如上文中说道：BooleanQuery中，文档打分值是所有域对应的打分值的和值。

### DisjunctionMaxQuery（tieBreakerMultiplier == 0）

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/7.png">

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/8.png">

&emsp;&emsp;对于文档0跟文档1，跟使用BooleanQuery获得打分值是一致的，他们不会tieBreakerMultiplier 的影响，因为这两篇文档分别只满足一个Query的条件。而对于文档2，它同时满足titleTermQuery跟bodyTermQuery这两个子Query后对应的文档分数分别为0.113950975跟0.082873434，由于tieBreakerMultiplier == 0，意味着最终文档2的打分值**只会**选择打分最高的子Query，对应的分数，即0.113950975。结合上文中给出的计算公式，其计算过程如下：


```java
        final float score = (float) (0.113950975 + 0.082873434 * 0);
```

&emsp;&emsp;正如上文中说道：如果某篇文档被多个子Query命中，虽然这篇文档的打分值在不同的子Query中是不同的，但是这篇文档的**最终打分值**（在收集器Collector中的打分值）会选择分数最高的。

### DisjunctionMaxQuery（tieBreakerMultiplier == 1）

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/9.png">

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）/10.png">

&emsp;&emsp;对于文档0跟文档1，他们不受tieBreakerMultiplier 的影响，因为这两篇文档分别只满足一个Query的条件。而对于文档2，它同时满足titleTermQuery跟bodyTermQuery这两个子Query后对应的文档分数分别为0.113950975跟0.082873434，由于tieBreakerMultiplier == 1，该值不为0，意味着文档2的打分值不但会选择子Query对应的打分值最高的那个，还会考虑其他子Query对应的打分值。结合上文中给出的计算公式，其计算过程如下：


```java
        final float score = (float) (0.113950975 + 0.082873434 * 1);
```


## 结语

&emsp;&emsp;基于篇幅原因，剩余的内容将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/DisjunctionMaxQuery/DisjunctionMaxQuery（一）.zip)下载附件



