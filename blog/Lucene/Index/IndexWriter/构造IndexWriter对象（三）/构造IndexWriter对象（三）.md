# [构造IndexWriter对象（三）](https://www.amazingkoala.com.cn/Lucene/Index/)

&emsp;&emsp;构造一个IndexWriter对象的流程总体分为下面三个部分：

- 设置索引目录Directory
- 设置IndexWriter的配置信息IndexWriterConfig
- 调用IndexWriter的构造函数

&emsp;&emsp;大家可以查看文章[构造IndexWriter对象（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1111/106.html)、[构造IndexWriter对象（二）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1114/107.html)来了解前两部分的内容，我们接着继续介绍最后一个部分，即调用IndexWriter的构造函数。

&emsp;&emsp;IndexWriter类有且仅有一个有参构造函数，如下所示：

```java
public IndexWriter(Directory d, IndexWriterConfig conf) throws IOException {
    ... ...
}
```

&emsp;&emsp;其中参数d以及conf正是分别由`设置索引目录Directory`、`设置IndexWriter的配置信息IndexWriterConfig`两部分获得。

# 调用IndexWriter的构造函数的流程图

图1：

<img src="构造IndexWriter对象（三）-image/1.png">

## 获取索引目录的索引文件锁

图2：

<img src="构造IndexWriter对象（三）-image/2.png">

&emsp;&emsp;该流程为Lucene使用索引文件锁对索引文件所在的目录进行加锁，使得同一时间总是只有一个IndexWriter对象可以更改索引文件，即保证单进程内(single in-process)多个不同IndexWriter对象互斥更改（多线程持有相同引用的IndexWriter对象视为一个IndexWriter不会受制于LockFactory，而是受制于对象锁（synchronized(this)）、多进程内(multi-processes)多个对象互斥更改。

&emsp;&emsp;更多关于索引文件锁的介绍可以看文章[索引文件锁LockFactory](https://www.amazingkoala.com.cn/Lucene/Store/2019/0604/62.html)。

## 获取封装后的Directory

图3：

<img src="构造IndexWriter对象（三）-image/3.png">

&emsp;&emsp;该流程中我们需要对Directory通过[LockValidatingDirectoryWrapper](https://www.amazingkoala.com.cn/Lucene/Store/2019/0615/67.html)对象进行再次封装， 使得在对索引目录中的文件进行任意形式的具有"破坏性"（destructive）的文件系统操作前尽可能（best-effort）确保索引文件锁是有效的（valid）。

&emsp;&emsp;在索引目录中的"破坏性"的文件系统操作包含下面几个内容：

- deleteFile(String name)方法：删除索引目录中的文件操作
- createOutput(String name, IOContext context)方法：在索引目录中创建新的文件
- copyFrom(Directory from, String src, String dest, IOContext context)方法：在索引目录中，将一个文件中的内容src复制到同一个索引目录中的另外一个不存在的文件dest
- rename(String source, String dest)方法：索引目录中的文件重命名操作
- syncMetaData()方法：磁盘同步操作
- sync(Collection\<String\> names)方法：磁盘同步操作

## 获取IndexCommit对应的StandardDirectoryReader

图4：

<img src="构造IndexWriter对象（三）-image/4.png">

&emsp;&emsp;如果IndexWriter的配置信息IndexWriterConfig设置了IndexCommit配置，那么我们需要获得描述IndexCommit中包含的信息的对象，即StandardDirectoryReader，生成StandardDirectoryReader的目的在后面的流程中会展开介绍，这里只要知道它的生成时机即可。

&emsp;&emsp;IndexCommit的介绍可以查看文章[构造IndexWriter对象（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1111/106.html)，而StandardDirectoryReader的介绍可以查看[近实时搜索NRT](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)、[SegmentReader](https://www.amazingkoala.com.cn/Lucene/Index/2019/1014/99.html)系列文章，这里不赘述。

## 根据不同的OpenMode执行对应的工作

图5：

<img src="构造IndexWriter对象（三）-image/5.png">

&emsp;&emsp;从图5中可以看出，尽管Lucene提供了三种索引目录的打开模式，但实际上只有CREATE跟APPEND两种打开模式的逻辑，三种模式的介绍可以看文章[构造IndexWriter对象（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1111/106.html)，这里不赘述。

&emsp;&emsp;在源码中，使用一个布尔值indexExists来描述图5中的流程点`索引目录中是否已经存在旧的索引？`，如果存在，那么indexExists的值为true，反之为false。indexExists在后面的流程中会被用到。

&emsp;&emsp;下面我们分别介绍`执行CREATE模式下的工作`、`执行APPEND模式下的工作`这两个流程。

### 执行CREATE模式下的工作的流程图

图6：

<img src="构造IndexWriter对象（三）-image/6.png">

#### 配置检查1

图7：

<img src="构造IndexWriter对象（三）-image/7.png">

&emsp;&emsp;该流程会检查用户是否正确设置了IndexCommit跟OpenMode两个配置，由于代码比较简单，故直接给出：

```java
if (config.getIndexCommit() != null) {
    // 条件一
    if (mode == OpenMode.CREATE) {
        throw new IllegalArgumentException("cannot use IndexWriterConfig.setIndexCommit() with OpenMode.CREATE");
    // 条件二
    } else {
        throw new IllegalArgumentException("cannot use IndexWriterConfig.setIndexCommit() when index has no commit");
    }
}
```

&emsp;&emsp;上面的代码描述的是在设置了配置IndexCommit之后对OpenMode进行配置检查：

- 条件一：如果用户设置的OpenMode为CREATE，由于该模式的含义是生成新的索引或覆盖旧的索引，而设置IndexCommit的目的是读取已经有的索引信息，故这两种是相互冲突的逻辑，Lucene通过抛出异常的方法来告知用户不能这么配置
- 条件二：如果用户设置的OpenMode为CREATE_OR_APPEND，由于通过图5中的流程点`索引目录中是否已经存在旧的索引？`判断出indexExists的值为false，即索引目录中没有任何的提交，但用户又配置了IndexCommit，这说明用户配置的IndexCommit跟IndexWriter类的有参构造函数中的参数d必须为同一个索引目录

#### 初始化一个新的SegmentInfos对象

图8：

<img src="构造IndexWriter对象（三）-image/8.png">

&emsp;&emsp;该流程只是描述了生成SegmentInfos对象的时机点，没其他多余的内容。

&emsp;&emsp;**SegmentInfos是什么：**

- SegmentInfos对象是[索引文件segment_N](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0610/65.html)以及[索引文件.si](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0605/63.html)在内存中的描述，可以看文章[近实时搜索NRT（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0916/93.html)中关于流程点`获得所有段的信息集合SegmentInfos`的介绍，这里不赘述

#### 同步SegmentInfos的部分信息

图9：

<img src="构造IndexWriter对象（三）-image/9.png">

&emsp;&emsp;如果索引目录中已经存在旧的索引，那么indexExists的值为true，那么我们先需要获得旧的索引中的最后一次提交commit中的三个信息，即version、counter、generation：

- version：该值用来描述SegmentInfos发生改变的次数，即索引信息发生改变的次数
- counter：新生成的索引文件segment_N的N值的需要用到该值
- generation：