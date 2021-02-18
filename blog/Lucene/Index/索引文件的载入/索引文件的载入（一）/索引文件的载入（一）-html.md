# [索引文件的载入（一）](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.4.0、8.6.0、8.7.0）

&emsp;&emsp;在文章[SegmentReader（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1014/99.html)中，我们介绍了SegmentReader对象，它用于描述一个段中的索引信息，并且说到SegmentReader对象中包含了一个SegmentCoreReaders对象。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/1.png">

&emsp;&emsp;图1中，<font color=blue>蓝框</font>标注的两个对象用于描述DocValues的索引信息，而<font color=red>红框</font>标注的SegmentCoreReader则描述了下面的索引信息，注意的是在文章[SegmentReader（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1014/99.html)中是基于Lucene 7.5.0的：

表一：

| 对象               | 描述                                                         |
| :----------------- | :----------------------------------------------------------- |
| StoredFieldsReader | 从[索引文件fdx&&fdt](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0301/38.html)中读取存储域的索引信息 |
| FieldsProducer     | 从[索引文件tim&&tip](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/43.html)、[索引文件doc](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/42.html)、[索引文件pos&&pay](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/41.html)中读取域的倒排信息 |
| TermVectorsReader  | 从[索引文件tvx&&tvd](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0429/56.html)读取词向量的索引信息（用于高亮优化查询） |
| PointsReader       | 从[索引文件dim&&dii](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0424/53.html)中读取域值为数值类型的索引信息 |
| NormsProducer      | 从[索引文件nvd&&nvm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0305/39.html)中读取域的打分信息（作为对文档进行打分的参数） |
| FieldInfos         | 从[索引文件fnm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0606/64.html)读取域的信息 |

&emsp;&emsp;在文章[SegmentReader（一）](https://www.amazingkoala.com.cn/Lucene/Index/2019/1014/99.html)中，并没有对每一种索引文件进行详细的读取过程的介绍，故`索引文件的载入`的系列文章对此将详细的展开。

&emsp;&emsp;该系列文章将要介绍的内容可以概述为这么一句话：**在初始化一个读取索引信息的reader期间，索引文件如何被读取（载入）**。由于只是初始化一个reader，而不是处于一个查询阶段，所以只有**部分索引文件的信息**会被载入到内存中。

## 索引文件的载入顺序

&emsp;&emsp;在SegmentCoreReader类的构造函数中可以看出表一中对应的索引文件的载入顺序：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/2.png">

&emsp;&emsp;上文中说到，SegmentCoreReader对象是被包含在SegmentReader对象中，故在SegmentReader类的构造函数中，还可以看出DocValues对应的索引文件跟SegmentCoreReader对象中包含的索引文件的载入顺序：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/3.png">

&emsp;&emsp;从图3可以看出，在构造SegmentReader对象时，先载入SegmentCoreReader对象，即表一中包含的索引文件， 随后再载入DocValues对应的索引文件。

&emsp;&emsp;**为什么图3中第98行处又载入了索引文件.fnm，在图2中不是已经载入过了吗？**

&emsp;&emsp;图3跟图2中载入的索引信息是不相同的，他们的区别在于索引文件.fnm的版本不同。这两个索引文件的区别在文章[构造IndexWriter对象](https://www.amazingkoala.com.cn/Lucene/Index/2019/1205/114.html)中介绍流程点`更新SegmentInfos的metaData`中详细介绍了，不赘述。

&emsp;&emsp;由于会依赖之前写过的跟索引文件的数据结构相关的文章，而那些文章又是依赖不同的Lucene版本，故注意版本区分。

&emsp;&emsp;**为什么之前介绍索引文件的文章会依赖不同的版本**

&emsp;&emsp;原因是Lucene版本迭代更新太快，索引文件的数据结构一直在优化，由于本人精力有限，无法对每一次的数据结构的优化重新写文章。

## 索引文件fdx&&fdt&&fdm的载入（Lucene 8.6.0）

&emsp;&emsp;描述存储域的[索引文件fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)的载入顺序依次如下所示：

```text
.fdt --> .fdm --> fdx --> .fdt
```

### 索引文件.fdt的载入

&emsp;&emsp;索引文件.fdt中存放了存储域的信息，载入过程中只会将部分信息读入到内存，如下所示：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/4.png">

&emsp;&emsp;图4中，<font color=red>红框</font>标注的字段将会被读取到内存中。

&emsp;&emsp;对于Header、ChunkSize、PackedIntsVersion字段，可以从索引文件的第一个字节依次读取出这三个字段。下面是读取这三个字段的代码：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/5.png">

&emsp;&emsp;**图4中的Footer字段如何读取？**

&emsp;&emsp;由于Footer占用固定的16个字节，随后根据索引文件.fdt的总长度，那么这两者的差值就是Footer字段在索引文件.fdt中的起始读取位置。

### 索引文件.fdm的载入

&emsp;&emsp;索引文件.fdm中的内容将被全量读取到内存中，如下所示：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/6.png">

### 索引文件.fdx的载入

&emsp;&emsp;索引文件.fdx的Header、PackedIntsVersion、Footer先被读取到内存中：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/7.png">

&emsp;&emsp;在图6中，从索引文件.fdm中读取了NumDocsIndex、StartPointsIndex字段，这两个字段用来描述图7中的NumDocs字段在索引文件.fdx中的位置区间。同理图6中读取的StartPointsIndex、SPEndPointer用来描述图7中StartPoints字段在索引文件.fdx中的位置区间。随后将NumDocs、StartPoints这两个字段的信息读取到内存中。

&emsp;&emsp;所以索引文件.fdx也是全量读取到内存的。

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/8.png">

### 索引文件.fdt的载入

&emsp;&emsp;图4中，索引文件.fdt的ChunkCount、DirtyChunkCount字段也要被读取到内存中，通过图6中从索引文件.fdm中读取的maxPointer来获取这两个字段在索引文件.fdt中的起始读取位置：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的载入/索引文件的载入（一）/9.png">

&emsp;&emsp;至此可以看出，对于描述存储域的索引文件fdx&&fdt&&fdm，除了索引文件.fdt的Chunk字段，其他索引文件的所有字段都会被读取到内存中。

&emsp;&emsp;对于索引文件fdx&&fdt&&fdm详细的读取过程可以阅读系列文章[索引文件的读取（十四）之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/Search/2020/1102/174.html)，该系列的文章介绍了索引文件.fdt的Chunk字段的读取方式。

## 结语

&emsp;&emsp;基于篇幅，其他索引文件的载入将在后面的文章中展开。



