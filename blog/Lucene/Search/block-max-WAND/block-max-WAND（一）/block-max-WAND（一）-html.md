# [block-max WAND（一）](https://www.amazingkoala.com.cn/Lucene/Search/)（Lucene 8.4.0）

&emsp;&emsp;从Lucene 8.0.0开始，Lucene新增了block-max WAND（Weak AND）算法，用于优化TopN的查询。该算法的引入可谓是一波三折，可以查看作者[Adrien Grand](https://www.elastic.co/cn/blog/author/adrien-grand)对该算法的介绍：https://www.elastic.co/cn/blog/faster-retrieval-of-top-hits-in-elasticsearch-with-block-max-wand，下文中将围绕这篇博客展开介绍。

## TopN查询（Lucene 7.5.0）

&emsp;&emsp;我们先介绍下在Lucene 8.0.0之前，如果实现TopN查询，假设有以下的搜索条件：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/block-max-WAND/block-max-WAND（一）/1.png">

&emsp;&emsp;图1中，第57行、58行、59行描述了只要文档中至少包含一个域名为"author"、域值为"lily"或者"lucy"的信息，那么该文档就满足查询条件；第60行代码描述了，我们只需要根据文档打分返回Top3的结果。

&emsp;&emsp;假设索引目录中一共有六篇文档，每篇文档的打分如下所示：

表一：

| 文档号 | Score1（lily） | Score2（Lucy） | 文档打分值（总分） |
| :----: | :------------: | -------------- | :----------------: |
|   0    |       0        | 0              |         0          |
|   1    |       5        | 4              |         9          |
|   2    |       6        | 7              |         13         |
|   3    |       3        | 8              |         11         |
|   4    |       2        | 6              |         8          |
|   5    |       3        | 3              |         6          |

&emsp;&emsp;表一中，Score1描述的是域名为"author"、域值为"lily"对应的文档打分值，同理Score2，最终文档的打分值为Score1跟Score2的和值。

&emsp;&emsp;结合图1跟表一可知，当我们处理到文档3，已经获得了Top3的搜索结果，即文档1、文档2、文档3。然而在Lucene 8.0.0之前，需要对所有满足图1的查询条件的文档进行打分，然后在Collector中使用根据score排序的优先级队列来维护Top3（见文章[Collector（二）](https://www.amazingkoala.com.cn/Lucene/Search/2019/0813/83.html)）。

&emsp;&emsp;通过上文的描述我们可以知道， 在查询过程中，一些打分值很低的文档号也被处理了，那有没有什么方式可以使得尽量跳过那些打分值较低的文档。

## MAXSCORE

&emsp;&emsp;在2012，Stefan Pohl介绍了[MaxScore](https://dl.acm.org/doi/10.1016/0306-4573%2895%2900020-H)算法，该算法大意为：如果我们想查找一些文档，查询条件为这些文档中至少包含"elasticsearch"或者"kibana"，并且根据文档打分值排序获得Top10。如果**能知道**根据elasticsearch关键字的文档打分**最大值**为3、根据kibana关键字的文档打分最大值为5，当Top10中的第10篇文档，即分数最低的那篇文档的的打分值为3时，那么在随后的处理中，我们就**只需要**处理包含"kibana"的文档集合（因为那些**只**包含"elasticsearch"的文档肯定是进不了Top10的），并且只需要判断"elasticsearch"是否在这些文档集合中，如果在那么参与打分，并且可能更新Top10。

&emsp;&emsp;实现该算法需要两个集合：

- 第一个集合：该集合中的任意一个term，该term在文档中的打分最高值比TopN中的最小值大
  - 这些term用来确定遍历的文档集合，即只需要处理包含第一个集合中的term的文档集合，例如上文中的"kibana"
- 第二个集合：该集合中的任意一个term，该term在文档中的打分值最高值比TopN中的最小值小
  - 这些term用来对文档打分， 用于更新TopN中的最低分

&emsp;&emsp;随着更多的文档的被处理，TopN中最低的文档打分值如果变高了，那么对于第一个集合中的某些term，如果它们在文档中的打分最高值比TopN中的最小值小，就将它们从第一个集合中移除，并添加到第二个集合中，这样使得进一步减少待处理的文档数量。

&emsp;&emsp;该算法在Lucene中无法直接应用，原因如下所示：

```text
Stefan didn't only describe the algorithm. A couple days before the conference, he opened a ticket against Lucene, where he shared a prototype that he had built. This contribution was exciting but also challenging to integrate because we would need a way to compute the maximum score for every term in the index. In particular, scores depend on index statistics such as document frequency (total number of documents that contain a given term), so adding more documents to an index also changes the maximum score of postings in existing segments. Stefan's prototype had worked around this issue by requiring a rewrite of the index, which meant this optimization would only work for static indexes in practice. This is a limitation that we weren't happy with, but addressing it would require a lot of work.
```

&emsp;&emsp;上文中，Lucene团队不接受这种Stefan提出的issues主要是该算法需要在索引（index）阶段需要计算所有term在所有包含它的文档的文档打分值、需要变更已经生成的段文件。

&emsp;&emsp;感兴趣的同学可以看这里的详细介绍：https://issues.apache.org/jira/browse/LUCENE-4100 。

## WAND

&emsp;&emsp;WAND（Weak AND）同样是一种可用于查询TopN的算法，然而该算法的实现同Lucene中的MinShouldMatchSum（minShouldMatch > 1）是相同的。我们简单的介绍下MinShouldMatchSum的实现方式，例如以下的查询条件将会使用MinShouldMatchSum：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/block-max-WAND/block-max-WAND（一）/2.png">

&emsp;&emsp;图2中的查询条件有三个子查询组成，描述的是：满足查询条件的文档中必须至少域名为"author"，域值为"lily"、"lucy"、"good"中的任意2个（即代码64行设置的minShouldMatch为2）。

&emsp;&emsp;MinShouldMatchSum算法实现中有三个核心的容器，分别是lead、head、tail，由于源码中关于这几个容器的注释，我怎么翻译都感觉不行😅，随意还是贴上原文朋友们自己品下：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/block-max-WAND/block-max-WAND（一）/3.png">

&emsp;&emsp;图3中，sub scorers中的每一个scorer可以简单的理解为满足某个子查询的文档信息，以图2为例，对于代码61行的子查询，他对应的scorer描述的是包含域名为"author"、域值为"lily"的文档信息。

&emsp;&emsp;在继续介绍之前，我们先理解下源码中的这么一段注释：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/block-max-WAND/block-max-WAND（一）/4.png">

&emsp;&emsp;图4的源码中说到，如果有n个SHOULD查询（比如图2中就是3个SHOULD查询），其中minShouldMatch的值为m，那么这种查询的开销为 n - m + 1个scorer的遍历开销，即我们不需要遍历n个scorer的文档信息就可以获得查询结果。另外图4中的某个查询的cost描述的是满足该查询的文档数量。

&emsp;&emsp;其推导思想说的是，包含n个子查询c1，c2，... cn且minShouldMatch为m的BooleanQuery，它可以转化为：

```text
(c1 AND (c2..cn | msm = m - 1)) OR (!c1 AND (c2..cn | msm = m))，两块部分通过"或的关系"（OR）组合而成
```

- (c1 AND (c2..cn|msm=m-1)) ：第一块部分描述了满足BooleanQuery查询要求的文档，如果**满足**子查询c1，那么必须（AND）至少满足c2..cn中任意m-1个子查询
-  (!c1 AND (c2..cn|msm=m))：第二块部分描述了满足BooleanQuery查询要求的文档，如果**不满足**子查询c1，那么必须（AND）至少满足c2..cn中任意m个子查询
	- 根据两块部分的组合关系（OR），**BooleanQuery的开销cost是这两部分的开销和**
- 假设子查询c1，c2，... cn是按照cost（上文中已经介绍，即满足子查询的文档数量）**升序排序**的，那么对于第一块部分(c1 AND (c2..cn|msm=m-1)) ，由于c1的cost最小，并且必须满足c1的查询条件，那么我们只需要遍历满足ci的文档集合即可（见文章[文档号合并（MUST）](https://www.amazingkoala.com.cn/Lucene/Search/2018/1218/27.html)），**所以第一块部分的开销就是c1的开销**
- 对于第二块部分(!c1 AND (c2..cn|msm=m))，它可以转化为一个包含 n -1 个子查询c2，... cn且minShouldMatch为m的**子BooleanQuery**，所以它又可以转化为(c2 AND (c3..cn|msm=m-1)) OR (!c2 AND (c3..cn|msm=m))
- 完整的类推如下所示：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/block-max-WAND/block-max-WAND（一）/5.png">

&emsp;&emsp;图5的推导图中的最后一个步骤中的$(!c_{n-m}\ AND\ (c_{n-m+1}\ ... c_n\ |\ msm=m))$描述的是一个包含 m 个子查询且minShouldMatch为m的子BooleanQuery，那么很明显我们只要遍历任意1个子查询对应的文档集合即可。

&emsp;&emsp;故最后得出我们只需要处理 n - m + 1个scorer。

&emsp;&emsp;基于这个理论就设计出了上文中说到的head跟tail，我们先看下源码中如何定义这两个容器：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/block-max-WAND/block-max-WAND（一）/6.png">

&emsp;&emsp;我们不用关心DisiWrapper跟DisiPriorityQueue是什么，统称为容器即可。初始化head时的容器大小即 n - m + 1（scorers.size() - minShouldMatch + 1），而tail的大小为 m - 1，即剩余的scorer丢到tail中，head跟size中的元素数量和为n。

&emsp;&emsp;结合上文介绍跟图3的注释就可以明白了，head中存放了用于遍历的scorers（注释中的in order to move quickly to the next candidate正说明了这点），而lead中存放了head中每一个scorer目前正在处理的相同的文档信息，并计算相同的文档信息的数量，如果该值大于等于minShouldMatch的值的话，说明文档满足查询条件。

&emsp;&emsp;上文的描述只是粗略的介绍了MinShouldMatchSum，在后面的文章中会详细介绍。在本中我们只需要理解为什么要设计head、tail既可以。

## block-max WAND

&emsp;&emsp;基于篇幅，剩余的内容将在下一篇文章中展开。

## 结语 

&emsp;&emsp;无

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/block-max-WAND/block-max-WAND（一）/block-max-WAND（一）.zip)下载附件

