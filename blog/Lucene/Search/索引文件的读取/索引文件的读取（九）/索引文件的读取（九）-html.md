---
title: 索引文件的读取（九）之tim&&tip（Lucene 8.4.0）
date: 2020-08-10 00:00:00
tags: [index,tim,tip]
categories:
- Lucene
- Search
---

&emsp;&emsp;本文承接文章[索引文件的读取（八）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Search/2020/0805/索引文件的读取（八）之tim&&tip)，继续介绍剩余的流程点，先给出流程图：

## 获取满足TermRangeQuery查询条件的term集合的流程图

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/1.png">

### 获取迭代器IntersectTermsEnum

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/2.png">

&emsp;&emsp;IntersectTermsEnum类继承于抽象类TermsEnum，TermsEnum在Lucene中到处可见，它封装了一个段中term的相关操作，下面罗列了TermsEnum类中的几个常用/常见的方法：

#### TermsEnum

##### 判断段中是否存在某个term

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/3.png">

&emsp;&emsp;seekExact(BytesRef text)方法用来判断term是否在当前段中，其中参数text即term，**下文中参数text统称为term**。

##### 获取迭代状态

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/4.png">

&emsp;&emsp;根据某个term判断当前迭代状态，一共有三种迭代状态，如下所示：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/5.png">

##### 根据ord判断段中是否存在某个term

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/6.png">

&emsp;&emsp;根据ord值判断段中是否存在某个term，ord描述了term之间的大小关系，在文章[索引文件的读取（五）之dvd&&dvm](https://www.amazingkoala.com.cn/Lucene/Search/2020/0714/索引文件的读取（五）之dvd&&dvm)中我们介绍了ord跟term的内容，这里不赘述。

##### 获取当前term 的ord值

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/7.png">

&emsp;&emsp;根据迭代器目前迭代位置对应的term找到该term对应的ord值。

##### 获取当前term的docFreq、totalTermFreq

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/8.png">

&emsp;&emsp;docFreq即包含当前term的文档数量，totalTermFreq为当前term在每一篇文档中出现的次数的总和。

#### IntersectTermsEnum

&emsp;&emsp;在获取迭代器IntersectTermsEnum的过程中，即在IntersectTermsEnum的构造函数中，本篇文章中我们只关心其中的一个逻辑，那就是初始化IntersectTermsEnumFrame对象，它用来描述段中第一个term所在的NodeBlock（见文章[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/索引文件之tim&&tip)）的信息，生成该对象的过程即读取[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/索引文件之tim&&tip)的过程：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/9.png">

&emsp;&emsp;在文章[索引文件的读取（七）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Search/2020/0804/索引文件的读取（七）之tim&&tip)中我们说到，索引文件.tim&&.tip中的RootCodeLength跟RootCodeValue字段会用于生成FiledReader对象，在构造该对象期间，会根据这两个字段计算出long类型的rootBlockFP，它就是前缀为"\[ \]"的PendingBlock合并信息（见文章[索引文件的生成（六）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Index/2020/0115/索引文件的生成（六）之tim&&tip)），当rootBLockFP执行右移两个bit的操作后，就获得了fp，它指向了段中第一个term所属的NodeBlock在索引文件.tim中的起始读取位置，随后读取NodeBlock中的信息，这些信息用于生成IntersectTermsEnumFrame对象。

### 获取Term并判断是否满足查询条件

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/10.png">

&emsp;&emsp;在这个两个流程点中核心的两个步骤为**获取term**跟**判断term是否满足查询条件**。

#### 获取term

&emsp;&emsp;在图9中，我们已经获得了第一个term所属的NodeBlock，如果想要获取下一个term，根据Suffix字段的数据结构，获取方式有所不同，我们先看下图9中Suffix字段的数据结构，它具有两种类型：

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/11.png">

&emsp;&emsp;当读取到类型一的数据结构时，意味着term的信息都在**当前NodeBlock**中，当读取到类型二的数据结构，意味着term信息在**其他NodeBlock**中，**注意的是：Length字段是一个组合编码，该字段最后一个bit为0时说明Suffix为类型一，为1时候Suffix为类型二**。

&emsp;&emsp;在文章[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/索引文件之tim&&tip)中已经给出了例子，我们这里再次列出：

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/12.png">

图13：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/13.png">

&emsp;&emsp;图13中可以看出，前缀为"ab"的信息在另一个NodeBlock中，至于为什么会分布在另一个NodeBlock中，见文章[索引文件的生成（六）之tim&&tip](https://www.amazingkoala.com.cn/Lucene/Index/2020/0115/索引文件的生成（六）之tim&&tip)的介绍。

#### 判断term是否满足查询条件

&emsp;&emsp;判断的逻辑用一句话可以总结：**在DFA（Deterministic Finite Automaton）中，如果term的每个字符（label）能根据转移函数从当前状态转移到下一个状态，并且要求字符的当前状态为前一个字符的下一个状态，并且最后一个字符对应的下一个状态为可接受状态，那么term满足查询条件**。

&emsp;&emsp;上述逻辑的例子在文章[Automaton](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0417/Automaton)中已经介绍，不赘述。

&emsp;&emsp;到这里，我们应该会提出这样的疑问，既然知道了TermRangeQuery的查询范围minValue、maxValue，为什么不直接通过简单的字符比较来判断term是否满足查询条件，简单的字符比较例如[FutureArrays.compareUnsigned](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-8.4.0/lucene/core/src/java/org/apache/lucene/util/FutureArrays.java)方法，从最高位的字符开始按照字典序进行比较，这块内容将在下一篇文章中展开。

### 收集Term

图14：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/索引文件的读取/索引文件的读取（九）/14.png">

&emsp;&emsp;收集满足查询条件的term后，如果term的数量超过某个阈值，处理方式也是不同的，在源码中，通过BOOLEAN_REWRITE_TERM_COUNT_THRESHOLD定义阈值，默认值为[16](https://issues.apache.org/jira/browse/LUCENE-6458)。

#### 未达到阈值

&emsp;&emsp;term的数量不大于16，那么TermRangeQuery转变为BooleanQuery，其中每个term生成TermQuery，作为BooleanQuery的子查询，并且TermQuery之间的关系为[SHOULD](https://www.amazingkoala.com.cn/Lucene/Search/2018/1211/BooleanQuery)。

#### 达到阈值

&emsp;&emsp;基于篇幅，这块内容将在下一篇文章中展开。

## 结语

&emsp;&emsp;无

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/索引文件的读取（九）/索引文件的读取（九）.zip)下载附件