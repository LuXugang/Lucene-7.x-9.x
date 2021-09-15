# [段内排序IndexSort](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.9.0）

&emsp;&emsp;段内排序IndexSort是Lucene在索引（Indexing）阶段提供的一个功能，该功能使得在执行[flush](https://www.amazingkoala.com.cn/Lucene/Index/2019/0716/74.html)、[commit](https://www.amazingkoala.com.cn/Lucene/Index/2019/0906/91.html)或者[NRT](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)操作后，新生成的段其包含的文档是有序的，即在索引阶段实现了文档的排序。

&emsp;&emsp;在之前的一些文章中已经简单的介绍了IndexSort，例如在文章[构造IndexWriter对象（一） ](https://www.amazingkoala.com.cn/Lucene/Index/2019/1111/106.html)说到，通过在IndexWriter的配置信息中添加IndexSort信息来开启段内排序的功能；在文章[文档提交之flush（三）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0725/76.html)中提到了对文档进行段内排序的时机点；在文章[Collector（三）](https://www.amazingkoala.com.cn/Lucene/Search/2019/0814/84.html)中提到了在Search阶段，如何通过IndexSort实现高效（提前结束）收集满足查询条件的文档集合。

&emsp;&emsp;本系列文章将会详细介绍IndexSort在索引阶段相关的内容，以及它将如何影响索引文件的生成、段的合并、以及在查询阶段的用途。

## IndexSort的应用

&emsp;&emsp;我们先通过一个例子来了解如何使用IndexSort这个功能。完整的demo地址见：https://github.com/LuXugang/Lucene-7.5.0/blob/master/LuceneDemo8.9.0/src/main/java/index/IndexSortTest.java 。 

图1：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/1.png">

图2：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/2.png">

&emsp;&emsp;图1中的第44、45行代码定义了两个排序规则。在继续展开介绍之前，我们先简单的说下Lucene中正排索引SortedSetDocValuesField（见图2）的一些概念。

### SortedSetDocValuesField

&emsp;&emsp;使用SortedSetDocValuesField可以使得我们在同一篇文档中定义一个或多个具有**相同域名**、**不同域值**的SortedSetDocValuesField域。这种域的其中一个应用方式即在索引阶段，对于一篇文档，我们可以选择其包含的SortedSetDocValuesField域的某一个域值参与段内排序。

&emsp;&emsp;例如在图2中，文档3中（代码第80、81行）定义了2个域名为"sort0"，域值分别为"b1"、"b2"的SortedSetDocValuesField域。并且在图1中的第44行代码定义了一个段内排序规则，该规则描述的是每个文档会使用域名为"sort0"，并且将最小的域值来参与排序。那么对于文档3，它将使用域值为"b1"（字符串使用字典序进行排序）参与段内排序。

#### SortedSetSelector.Type

&emsp;&emsp;图2中SortedSetSelector.Type.MIN规定了使用最小的域值参与段内排序。SortedSetSelector.Type的所有选项如下所示：

图3：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/3.png">

&emsp;&emsp;图3中MIN、MAX就不做介绍了，我们说下MIDDLE_MIN跟MIDDLE_MAX。

##### MIDDLE_MIN、MIDDLE_MAX

&emsp;&emsp;如果域值的数量是**奇数**，那么MIDDLE_MIN、MIDDLE_MAX具有相同的作用，比如有以下的域值：

```
{"b1", "b2", "b3", "b4", "b5"}
```

&emsp;&emsp;那么将会选择"b3"参与排序。

&emsp;&emsp;如果域值的数量是**偶数**，假设有以下的域值：

```
{"b1", "b2", "b3", "b4", "b5", "b6"}
```

&emsp;&emsp;那么在MIDDLE_MIN条件下会选择"b3"、在MIDDLE_MAX下会选择"b4"。

&emsp;&emsp;另外使用SortedSetDocValuesField的一个场景是，我们在搜索阶段可以根据SortedSetDocValuesField对查询结果进行排序，并且可以通过指定不同的SortedSetSelector.Type获取不同的排序结果。

### 排序方式概述

&emsp;&emsp;我们接着看下图1。图1中定义了两个规则，那么在段内排序的过程中，先按照"sort0"进行排序，当无法比较出先后关系时，接着按照"sort1"进行排序，如果两个排序规则都无法比较出先后关系，则最终比较文档的添加顺序。

### 文档之间的排序比较方式

图4：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/4.png">

图5：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/5.png">

&emsp;&emsp;图4中，**我们的搜索条件没有对结果增加额外的排序规则，那么查询结果将会按照段内排序后的顺序输出**。

&emsp;&emsp;我们结合图2，分别介绍下一些文档之间排序比较的方式。

#### 文档0

&emsp;&emsp;这里说的文档0指的是图2中添加的顺序，如图2中第53行的代码所示。

&emsp;&emsp;根据"sort0"的排序规则，将按照域值从大到小排序（SortedSetSortField的第二个参数reverse的值为true），所以文档0~3这四篇文档将分别使用"c1"、"b1"、"b1"、"b1"进行比较，另外由于文档4中没有"sort0"域，那么它将被排到最末位置。可见根据"sort0"，**只能**确定文档0是排在最前面的以及文档4是排在最后面，如下所示。故需要通过"sort1"对文档1、2、3进一步排序。

```java
文档0 --> 文档1、2、3 --> 文档4
```

#### 文档1、2、3

&emsp;&emsp;根据"sort1"的排序规则，将按照域值从大到小排序，所以文档1、2、3这四篇文档将分别使用"e2"、"f2"、"e2"进行比较，可见文档2在这三篇文档中排在最前面，由于文档1、3无法通过"sort1"区分出先后关系，并且没有其他的排序排序规则了，那么由于文档1先被添加，故文档1排在文档3前面，如下所示：

```java
文档0 --> 文档2 --> 文档1 --> 文档3 --> 文档4
```

### 搜索阶段的排序

&emsp;&emsp;上文中说到SortedSetDocValuesField可以用于在索引阶段排序，同样的它也可以用于搜索阶段的排序。在设置了图1中的排序规则前提下，如果我们在搜索阶段提供了以下的排序规则：

图6：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/6.png">

&emsp;&emsp;其查询结果如下所示：

图7：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/7.png">

&emsp;&emsp;其排序的比较过程跟IndexSort是一样的，这里就不赘述了。

### 用于排序的域

&emsp;&emsp;下图中列出了其他可以用于段内排序的域：

图8：

<img src="https://www.amazingkoala.com.cn/uploads/lucene/index/段内排序IndexSort/段内排序IndexSort（一）/8.png">

&emsp;&emsp;这些域的使用方法可以通过查看每个类的注释就可以完全理解，故不展开介绍了。

## 结语

&emsp;&emsp;下一篇文章中，我们将介绍段内排序在索引阶段的排序方式、排序时机点以及其他相关内容。

[点击](https://www.amazingkoala.com.cn/attachment/Lucene/Index/段内排序IndexSort/段内排序IndexSort（一）.zip)下载附件
