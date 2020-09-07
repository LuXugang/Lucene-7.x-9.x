# [索引文件的读取（十二）](https://www.amazingkoala.com.cn/Lucene/Search/)（Lucene 8.4.0）

&emsp;&emsp;在前几篇索引文件的读取的系列文章中，我们介绍[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/43.html)的读取时机点时说到，在生成[StandardDirectoryReader](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)对象期间，会生成[SegmentReader](https://www.amazingkoala.com.cn/Lucene/Index/2019/1014/99.html)对象，该对象中的FieldsProducer信息描述了[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/43.html)、[索引文件doc](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/42.html)、[索引文件pos&&pay](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/41.html)中所有域的索引信息，故我们从本篇文章开始介绍索引文件.doc、.pos、.pay的读取。

## 索引文件.doc的数据结构（Lucene 8.4.0）

&emsp;&emsp;在文章[索引文件的生成（三）之跳表SkipList](https://www.amazingkoala.com.cn/Lucene/Index/2020/0103/123.html)跟[索引文件的生成（四）之跳表SkipList](https://www.amazingkoala.com.cn/Lucene/Index/2020/0106/124.html)中，我们基于Lucene 7.5.0介绍了跳表的数据结构，然而从Lucene 8.0.0开始，对跳表的数据的结构进行了调整，即对索引文件.doc的数据结构进行了调整，故在介绍索引文件.doc、.pos、.pay的读取之前，我们先介绍下调整目的以及调整后的数据结构。

### 为什么要调整

&emsp;&emsp;本文仅仅给出两个链接，它们分别介绍了在elasticSearch跟Lucene两个层面的调整初衷，感兴趣的同学可以自行查阅。当然在随后的内容中也会提及这两篇文章中介绍的部分内容：

- elastic：https://www.elastic.co/cn/blog/faster-retrieval-of-top-hits-in-elasticsearch-with-block-max-wand
- Lucene：https://issues.apache.org/jira/browse/LUCENE-4198

### 调整后的数据结构

&emsp;&emsp;我们先直接给出两个版本的索引文件.doc的数据结构：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/1.png">

[点击](http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/7_5_0.html)查看大图

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/2.png">

[点击](http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/8_4_0.html)查看大图

&emsp;&emsp;比较图1跟图2的区别可以看出，Lucene 8.4.0中所有level的SkipDatum字段都增加了Impacts跟ImpactLength两个字段，其中ImpactLength字段用于描述Impacts字段的长度，使得在读取阶段，能通过ImpactLength确定Impacts字段的信息在索引文件.doc中的读取区间。

#### Impact

&emsp;&emsp;Impact字段的数据结构有两种，由于使用差值存储，即图2中normValue实际存储的值为跟上一个normValue的差值，故当[normValue](https://www.amazingkoala.com.cn/Lucene/Index/2020/0828/164.html)的值可能为0，那么就不将该值写入到索引文件中，freq字段同样使用了[组合存储](https://www.amazingkoala.com.cn/Lucene/Index/2019/0222/36.html)，在读取阶段，根据freq对应的二进制值的最低bit来判断是否存储了normValue。freq跟normValue的介绍见下文。

**为什么存储freq跟normValue**

&emsp;&emsp;在文章[索引文件的生成（三）之跳表SkipList](https://www.amazingkoala.com.cn/Lucene/Index/2020/0103/123.html)中我们知道，在索引阶段，处理某个term的文档号跟词频信息期间，即生成索引文件.doc期间，每处理128篇文档号就会生成一个block，同时生成一条跳表信息，即SkipDatum，在Lucene 8.0.0之后，这个SkipDatum中额外多出字段，即Impacts，它存储了当前block中一个或多个（不超过128个）**具有竞争力（Competitive）**的文档的freq跟norm信息（原因见下文介绍）。**具有竞争力**描述的是在文档打分阶段，某些freq跟norm这一对（pair）信息对应的文档能获得较高（注意这里的用词，是"较高"，不是最高）的打分值，至于为什么能根据freq跟norm能计算出文档的打分值，在下文中我们再介绍。

##### 生成Impact的过程

&emsp;&emsp;在处理某个term对应的文档集合（包含term的文档集合）期间，每处理一篇文档， 就使用[CompetitiveImpactAccumulator](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/codecs/CompetitiveImpactAccumulator.java)对象来收集term在这篇文档中的freq跟normValue的信息，其中freq描述的是term在这篇文档中的词频，即出现的次数；normValue描述的是标准化后的文档的长度（文档的长度、标准化的概念见文章[索引文件的生成（二十二）之nvd&&nvm](https://www.amazingkoala.com.cn/Lucene/Index/2020/0828/164.html)）。

&emsp;&emsp;CompetitiveImpactAccumulator对象中使用int类型的数组maxFreqs[ ]收集freq和normValue，由于收集的代码十分简单，我们直接给出介绍：

```java
/** Accumulate a (freq,norm) pair, updating this structure if there is no
 *  equivalent or more competitive entry already. */
public void add(int freq, long norm) {
    if (norm >= Byte.MIN_VALUE && norm <= Byte.MAX_VALUE) {
        int index = Byte.toUnsignedInt((byte) norm);
    maxFreqs[index] = Math.max(maxFreqs[index], freq); 
    } else { // 这种情况我们暂时不考虑
        add(new Impact(freq, norm), otherFreqNormPairs);
    }
    assertConsistent();
}
```

&emsp;&emsp;上述代码中，参数norm即上文中的normValue。本文中，我们暂时只关心第4行到第6行的代码。可见maxFreq[ ]数组将normValue的值作为数组下标，freq作为数组元素，并且对于相同的normValue，只保存最大的freq。

&emsp;&emsp;通过上述的介绍，相信同学们会至少抛出下面的疑问：

- 疑问一：为什么能根据freq跟normValue计算出文档的打分值
- 疑问二：为什么在maxFreqs数组中，normValue可以用于数组的下标，并且相同的normValue，只保存最大的freq

**为什么能根据freq跟normValue计算出文档的打分值**

&emsp;&emsp;因为从Lucene 8.0.0开始，对文档进行打分的[score( )](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/search/similarities/Similarity.java)方法参数发生了变化，如下所示，**注意的是下文中出现的norm，即上文中的normValue**：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/3.png">

&emsp;&emsp;Lucene 8.0.0之前，在score( )方法中，其实是根据参数doc间接的获取norm，即normValue，然后再结合freq执行文档的打分，Lucene 8.0.0之后的改动使得在搜索阶段能更快的完成打分逻辑。

&emsp;&emsp;详细的介绍可以阅读以下两个issue：

- LUCENE-4198：https://issues.apache.org/jira/browse/LUCENE-4198
- LUCENE-8116：https://issues.apache.org/jira/browse/LUCENE-8116

**为什么在maxFreqs数组中，normValue可以用于数组的下标，并且相同的normValue，只保存最大的freq**

&emsp;&emsp;回答该问题，我们只需要通过了解打分公式的实现规范就可以得到答案，下图给出的是score方法的注释：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/4.png">

&emsp;&emsp;图4中红框中的注释大意为：如果norm相等，那么freq较大对应的文档打分值会更高，如果freq相等，那么norm较小对应的文档打分值会更高，另外在文章[索引文件的生成（二十二）之nvd&&nvm](https://www.amazingkoala.com.cn/Lucene/Index/2020/0828/164.html)中，我们说到，Lucene默认的BM25打分公式在通过computeNorm( )方法计算出的normValue值的可选区间为[1, 255]，如果让normValue作为maxFreqs数组的下标，那么可以使得该数组的长度是固定的。

&emsp;&emsp;结合上文中CompetitiveImpactAccumulator对象收集freq跟normValue的代码，可以看出，对于**相同的normValue**，freq越小，文档打分值就越低，即所谓的不具有竞争力，故只需要保存最大的freq。

&emsp;&emsp;我们假设在处理完某个term对应的128篇文档后，CompetitiveImpactAccumulator对象收集了如下的freq跟normValue信息，不过不是所有的信息都是具有竞争力的，我们需要依次遍历maxFreqs数组，挑选出一个或多个具有竞争力的信息：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/5.png">

&emsp;&emsp;由于normValue的值区间为[1, 255]，故图5中下标值为0的数组元素总是为0，并且从下标值为1的位置开始处理。

&emsp;&emsp;由于当前表1中没有信息，那么下标值1以及对应的数组元素freq = 4自动视为最具竞争力的：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/6.png">

&emsp;&emsp;接着观察下标值2以及对应的数组元素freq = 3，由上文中图4关于打分公式的介绍可知，freq较小，同时normValue较大，对应的文档打分值肯定是较小的，故不具有竞争力。同理下标值3以及对应的数组元素freq = 2，也是不具有竞争力的。

&emsp;&emsp;我们接着看下标值4以及对应的数组元素freq = 8，尽管normValue = 4 大于表1中的normValue = 1，但是freq = 8 大于表1中的freq = 4，故它是**有可能**对应的文档打分值是较高的，所以它具有竞争力：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/7.png">

&emsp;&emsp;接着观察下标值5以及对应的数组元素freq = 28，由于它比表中一freq的最大值8还要大，故它也是具有竞争力的：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（十二）/8.png">

&emsp;&emsp;经过上面的介绍，可以知道，下标值255以及对应的数组元素freq = 23 ，它肯定是不具有竞争力的。

&emsp;&emsp;至此我们知道，**在索引阶段，只是根据freq跟norm这一对信息粗略的选出一些具有竞争力的候选者，即并不会真正的调用score( )方法计算出文档的打分值，原因很明显，在索引阶段，需要处理包含term的每一篇文档号，此时对这些文档号执行打分操作在性能上是不现实的**。

&emsp;&emsp;至此我们可以真正的回答上文中提出的问题，即**为什么存储freq跟normValue**，原因就是这种方式使得在搜索阶段，能根据这些最具竞争力的freq跟norm信息，计算出一个block中的128篇文档的最高的文档打分值maxScore。通过这个maxScore使得一些类似TopN的查询能快速的在block中跳转，最终找到满足查询条件的文档号。在后面的文章中我们会详细的介绍如何通过Impact实现性能更高的查询，这里就简单的提一下。

## 结语

&emsp;&emsp;无

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/索引文件的读取（十二）/索引文件的读取（十二）.zip)下载附件



