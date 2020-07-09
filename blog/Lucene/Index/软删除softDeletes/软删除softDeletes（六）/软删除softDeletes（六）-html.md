# [软删除softDeletes（六）](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.4.0）

&emsp;&emsp;我们接着文章[软删除softDeletes（五）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0708/152.html)继续介绍合并策略SoftDeletesRetentionMergePolicy，在文章[近实时搜索NRT（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)中使用方法三&&方法四获取StandardDirectoryReader和文章[文档提交之flush（八）终](https://www.amazingkoala.com.cn/Lucene/Index/2019/0812/81.html)中执行流程点`更新ReaderPool的流程图`时，会判断一个段中的文档是否都被删除（软删除跟硬删除），如果为真，那么这个段对应的索引文件，也就是索引信息将从索引目录中物理删除（如果没有其他reader占用的话），但是如果使用了合并策略SoftDeletesRetentionMergePolicy，那么上述的两个场景也不会删除这个段，我们通过下面的例子来展开介绍。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/软删除softDeletes/软删除softDeletes（六）/1.png">

&emsp;&emsp;图1完整的demo见：https://github.com/LuXugang/Lucene-7.5.0/blob/master/LuceneDemo8.4.0/src/main/java/io/softDeletes/SoftDeletesTest9.java 。

&emsp;&emsp;图1中第62行执行了软删除的操作（<font color=red>红框</font>），那么包含域名为"author"、域值为"D0"的文档将被软删除，故文档0将被标记为软删除的，同时添加了一篇新的文档newDoc，即文档1，随后第63行执行了硬删除（<font color=blue>蓝色</font>），那么newDoc也会被删除，最终在第64行执行commit()后，生成一个段，这个段中仅有的两篇文档都是被删除的。

&emsp;&emsp;如果不使用该合并策略，即图1中useSoftMergePolice为false，那么由于这个段中所有的文档都是被删除的，故reader的两个方法numDoc()、maxDoc()都是0，同时索引目录中的索引文件如下所示：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/软删除softDeletes/软删除softDeletes（六）/2.png">

&emsp;&emsp;可见索引目录中没有跟数据相关的索引文件，也就是说刚刚生成的段被物理删除了。如果我们使用了第67行的合并策略，那么索引文件就不会被删除：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/软删除softDeletes/软删除softDeletes（六）/3.png">

&emsp;&emsp;同时图1中第67、68行，reader.numDoc()的值为0，描述的是段中的文档都是标记为删除的（软删除或硬删除）；reader.maxDoc()的值为2，描述了段中的文档的数量（包含删除跟未删除的）。同时我们可以看出，这个合并策略的功能不是局限于执行段的合并时候才发挥作用。

&emsp;&emsp;上述的差异主要是合并策略SoftDeletesRetentionMergePolicy中实现了下面的这个方法：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/软删除softDeletes/软删除softDeletes（六）/4.png">

&emsp;&emsp;实现原理很简单，即通过定义一个Query，例如图5中第46行的querySupplier，然后根据Query去段中查找，如果**至少找到一篇**满足查询要求的文档，那么就不会删除这个段，注意的是满足查询要求的文档既可以是被硬删除的也可以是被软删除的。

&emsp;&emsp;看这里有些同学可能会有疑问，好像上述的机制跟软删除没有什么关系，也就说如果段中文档都是被硬删除的，甚至IndexWriterConfig不设置软删除的域名，是否也能实现上述的功能呢，看下面的例子：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/软删除softDeletes/软删除softDeletes（六）/5.png">

&emsp;&emsp;图5完整的demo见：https://github.com/LuXugang/Lucene-7.5.0/blob/master/LuceneDemo8.4.0/src/main/java/io/softDeletes/SoftDeletesTest10.java 。

&emsp;&emsp;图5中第50、51、52的代码明确了我们没有设置软删除域，并且在第64、65执行了硬删除，即文档0、文档4都将被硬删除，然后使用了第45行定义的合并策略后，这个段仍然是被保留的。不过在常用的使用场景下没有什么实际意义，因为被硬删除的文档号被记录在索引文件.liv中，即使这个段被保留了下来，使用IndexSearcher.search()时候查询的过程中，这些文档也会被过滤掉（硬删除的文档满足查询条件，但是这篇文档的文档号在传给Collector之前会使用Bits对象过滤，Bits对象为索引文件.liv在内存中的描述方式）。

&emsp;&emsp;在文章[软删除softDeletes（五）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0708/152.html)中我们对SoftDeletesRetentionMergePolicy在源码中的注释划分了五段进行介绍，剩余未介绍还有第三段、第四段、第五段，其中第三、四段的例子在文章[软删除softDeletes（一）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0616/148.html)的第三个例子中介绍故了，不赘述，我们仅仅看下第五段的注释。

## 第五段

```
Using this merge policy allows to control when soft deletes are claimed by merges.
```
&emsp;&emsp;该段注释大意为：使用这个合并策略使得在合并期间，能控制何时处理被标记为软删除的文档。

&emsp;&emsp;该段注释实际是对前四段注释的总结，从上文以及前面的文章中我们可以看出，控制软删除文档处理时机主要依靠这个合并策略的Supplier\<Query\> retentionQuerySupplier参数，它可以用来控制被软删除的文档在合并后是否能继续保留在新的段中，控制软删除的文档的有效期（History retention）等。

## numDeletesToMerge

&emsp;&emsp;numDeletesToMerge描述的是一个段在合并之后，段中被删除的文档将被处理（claim）的数量。

&emsp;&emsp;由于SoftDeletesRetentionMergePolicy这个对象并没有真正的合并逻辑（即生成OneMerger对象，见文章[LogMergePolicy](https://www.amazingkoala.com.cn/Lucene/Index/2019/0513/58.html)），它只是封装了其他的合并策略，实现扩展功能，numDeletesToMerge作为实现扩展的功能之一，它影响了其他的合并策略的合并逻辑，例如如果封装了合并策略[LogMergePolicy](https://www.amazingkoala.com.cn/Lucene/Index/2019/0513/58.html)，该策略是根据段的大小（Segment Size）挑选待合并的段，Segment Size的定义通过下面两种方式来描述：

- 文档数量：一个段中的文档数量可以用来描述段的大小，例如LogDocMergePolicy（[LogMergePolicy](https://www.amazingkoala.com.cn/Lucene/Index/2019/0513/58.html)的子类）就使用了该方法来计算段的大小
- 索引文件大小：一个段中包含的所有的[索引文件](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/)大小总和，在Lucene7.5.0版本中除了LogDocMergePolicy，其他的合并策略都使用该方法

&emsp;&emsp;当使用`文档数量`作为Segment Size时，被删除的文档也会参数文档数量的计算，而SoftDeletesRetentionMergePolicy则可以根据需要更改numDeletesToMerge的值，从而影响Segment Size。实现方式跟之前文章介绍过的一样，即通过合并策略的Supplier\<Query\> retentionQuerySupplier参数来更改numDeletesToMerge。

## 结语

&emsp;&emsp;关于软删除的内容暂时就介绍这么多，欢迎各位同学指出文章中的错误。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/软删除softDeletes/软删除softDeletes（六）/软删除softDeletes（六）.zip)下载附件
