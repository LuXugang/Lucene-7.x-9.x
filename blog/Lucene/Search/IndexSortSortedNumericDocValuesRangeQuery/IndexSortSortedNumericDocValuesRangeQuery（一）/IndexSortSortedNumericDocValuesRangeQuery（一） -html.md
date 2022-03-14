# [IndexSortSortedNumericDocValuesRangeQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search)（Lucene 9.0.0）

&emsp;&emsp;我们先通过IndexSortSortedNumericDocValuesRangeQuery类的注释了解下这个Query。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/1.png">

&emsp;&emsp;图1中<font color=red>红框</font>标注的注释说到，范围查询可以通过利用[Index Sort](https://www.amazingkoala.com.cn/Lucene/Index/2021/0915/201.html)来提高查询效率。如果查询条件的域field正好是用于段内排序的域，那么就可以通过二分法找到满足查询条件的两个文档号。这两个文档号分别作为一个**区间**的上下界，满足查询条件的文档号肯定都在这个区间内。

&emsp;&emsp;对于<font color=red>红框</font>注释有两个细节需要注意：

- 用于段内排序的域可以设置多个排序规则，这些排序规则有先后顺序，但是只有第一个排序规则（primary sort）对应的域跟查询条件的域相同才能提高查询效率
- 上文中说到的**区间**，这个区间内的文档号不是都满足查询条件的，只能保证满足查询条件的文档号肯定都在这个区间内

&emsp;&emsp;下文中，我们将会这两个注意的细节作出详细的介绍。

&emsp;&emsp;图1中<font color=blue>蓝框</font>标注的注释说到，只有同时满足下面的条件才能实现这种优化执行策略（optimized execution strategy）：

- 条件一：索引是有序的，第一个排序规则对应的field必须跟查询条件的域相同
- 条件二：范围查询条件的域必须是[SortedNumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0410/47.html)或者[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)
- 条件三：段中每一篇文档中**最多**只能包含一个[SortedNumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0410/47.html)或者[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)

&emsp;&emsp;条件三的限制换个说话就是：一篇文档中要么不包含SortedNumericDocValues或者NumericDocValues，要么**只能**包含一个。具体原因在下文中说明。

&emsp;&emsp;如果上述三个条件不同时满足，那么就无法使用这个优化执行策略，这次查询就会交给fallbackQuery（下文会介绍）去执行。

&emsp;&emsp;图1中<font color=gray>灰框</font>标注的注释说道，fallbackQuery的查询条件必须跟IndexSortSortedNumericDocValuesRangeQuery一致，返回的结果必须是相同的文档集合并且每篇文档的分数是固定的。

&emsp;&emsp;因为IndexSortSortedNumericDocValuesRangeQuery的查询逻辑是先尝试用IndexSort机制进行查询，如果无法同时满足上文中说道的**三个条件**，那么就将这次查询委托（delegate）给fallbackQuery。

&emsp;&emsp;<font color=gray>灰框</font>标注的例子中可以看出，代码66行的LongPoint.newRangeQuery即fallbackQuery，它的查询条件跟代码67行的IndexSortSortedNumericDocValuesRangeQuery是一样的。

&emsp;&emsp;从这个例子也可以看出，fallbackQuery跟IndexSortSortedNumericDocValuesRangeQuery的查询条件保持一致需要使用者自己来保证，意味着用户想要自己编写一个效率较高的数值范围查询有较高的学习成本，他至少要了解并且在索引阶段写入[SortedNumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0410/47.html)或者[NumericDocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)，还要了解各种数值类型范围查询，比如[IndexOrDocValuesQuery](https://www.amazingkoala.com.cn/Lucene/Search/2021/0701/196.html)，[PointRangeQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search/2021/1122/202.html)等等。所以社区已经开始着手开发一些"sugar"域跟Query（见[LUCENE-10162](https://issues.apache.org/jira/browse/LUCENE-10162)），使得用户能简单透明的使用数值类型的范围查询，让Lucene来帮助用户生成一个高效的Query。

&emsp;&emsp;我们看下Elasticsearch 8.0的NumberFieldMapper类中是如何生成一个高效的数值类型范围查询Query的，以Mapping类型为int的字段为例：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/2.png">

&emsp;&emsp;图2描述了生成一个用于int类型的字段的范围查询的过程：首先通过IntPoint.newRangeQuery生成一个[PointRangeQuery](https://www.amazingkoala.com.cn/Lucene/Search/2021/1122/202.html)，我们称之为indexQuery；如果该字段开启了DocValues，那么通过SortedNumericDocValuesField.newSlowRangeQuery生成一个DocValues的范围查询，我们称之为dvQuery, 然后将indexQuery跟dvQuery封装为一个[IndexOrDocValuesQuery](https://www.amazingkoala.com.cn/Lucene/Search/2021/0701/196.html)；最后如果分片的索引根据查询域排序了，那么进一步将[IndexOrDocValuesQuery](https://www.amazingkoala.com.cn/Lucene/Search/2021/0701/196.html)封装为IndexSortSortedNumericDocValuesRangeQuery，此时[IndexOrDocValuesQuery](https://www.amazingkoala.com.cn/Lucene/Search/2021/0701/196.html)即上文中的fallbackQuery。

&emsp;&emsp;在文章[IndexOrDocValuesQuery](https://www.amazingkoala.com.cn/Lucene/Search/2021/0701/196.html)中详细的介绍了这个Query，本文中我们简单的提一下：IndexOrDocValuesQuery既利用了倒排中根据term快速获取满足查询条件的文档号集合能力，又利用了正排中根据文档号能快速check是否存在某个term的能力。使得IndexOrDocValuesQuery不管作为leader iterator还是follow iterator都能获得不错的读取性能。

&emsp;&emsp;回到图2中构造过程，的确需要相当大的学习成本才能构造成一个高效的Query。[LUCENE-10162](https://issues.apache.org/jira/browse/LUCENE-10162)的目标在于期望用户通过编写类似IntField.NumericRangeQuery(String field, long lowerValue, long upperValue)这种方式就可以获得图2中的Query。

## 利用IndexSort实现高效查询

&emsp;&emsp;不管是哪种Query的实现，其需要解决的最重要的核心问题是这个Query如何提供一个[迭代器DocIdSetIterator](https://www.amazingkoala.com.cn/Lucene/gongjulei/2021/0623/194.html)，迭代器中包含了满足查询条件的文档号集合，以及定义了读取这些文档号的方式。查询性能取决于迭代器的实现方式。

### BoundedDocSetIdIterator

&emsp;&emsp;BoundedDocSetIdIterator即在利用了IndexSort后，IndexSortSortedNumericDocValuesRangeQuery实现的迭代器，注意的是这个迭代器的名字有书写错误typo，应该是BoundedDocIdSetIterator，也许会在这个[LUCENE-10458](https://github.com/apache/lucene/pull/736)合并后修正。

&emsp;&emsp;我们看下这个迭代器中包含的信息，即该类的成员变量：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/3.png">

&emsp;&emsp;图3中的firstDoc、lastDoc中用来描述一个左闭右开的文档号集合的区间，这个区间内**至少**包含了满足查询条件的所有文档号。对于firstDoc跟lastDoc是如何计算的，我们将在下一篇文章中展开。

**为什么说BoundedDocSetIdIterator中至少包含了满足查询条件的所有文档号**

&emsp;&emsp;因为某些文档中虽然不包含查询条件对应的域的信息，但是Lucene会给这篇文档添加一个默认值来参与段内的文档排序。该默认值就是MissingValue。这些被设置了MissingValue的文档号就有可能被统计到迭代器中。

#### 例子

我们通过一个例子介绍下上述的问题，demo地址：https://github.com/LuXugang/Lucene-7.5.0/blob/master/LuceneDemo9.0.0/src/main/java/facet/MissingValueTest.java 。

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/4.png">

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/5.png">

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/6.png">

&emsp;&emsp;图4中，我们定义了一个IndexSortSortedNumericDocValuesRangeQuery的范围查询，其中代码66、67定义了查询条件的上下界分别为 1, 100，代码72行定义了查询条件的域名为"number"，显然，只有文档1跟文档2中包含"number"域，并且其域值满足查询条件，而文档0中不包含"number"域的信息，所以在图6中，满足查询条件的文档号分别是文档1跟文档2。

&emsp;&emsp;图4的例子满足了利用IndexSort的三个条件，故IndexSortSortedNumericDocValuesRangeQuery会生成图5中的BoundedDocSetIdIterator迭代器。但是通过断点可以看到firstDoc跟lastDoc组成的左闭右开的区间中的文档号集合包含了文档0、文档1、文档2。这是因为在图4中我们定义了MissingValue的值为3，所以文档0也被BoundedDocSetIdIterator收集了（其被收集的原因也会在下一篇文章中展开介绍）。

&emsp;&emsp;如果图4中不设置MissingValue，那么BoundedDocSetIdIterator的信息是这样的：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/7.png">

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）/8.png">

&emsp;&emsp;由于在设置了MissingValue后，BoundedDocSetIdIterator中可能会包含不满足查询条件的文档号，所以在BoundedDocSetIdIterator中，如图8所示，它还包含一个成员变量delegate，它是上文中fallbackQuery对应的迭代器。因为fallbackQuery的迭代器中包含的文档号肯定是满足查询条件的，所以在读取BoundedDocSetIdIterator的文档号时，每次都会去delegate中检查是否存在这个文档号，来保证返回数据的准确性。

## 结语

&emsp;&emsp;剩余内容将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/IndexSortSortedNumericDocValuesRangeQuery/IndexSortSortedNumericDocValuesRangeQuery（一）.zip)下载附件

