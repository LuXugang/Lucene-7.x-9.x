# [fnm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/)
&emsp;&emsp;索引文件.fnm用来描述域信息（FieldInfo）

# 例子
&emsp;&emsp;为了便于介绍.fnm中的各个字段，给出下面的例子

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fnm/1.png">


# fnm文件的数据结构


图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fnm/2.png">

## FieldsCount
&emsp;&emsp;FieldsCount描述的是.fnm中域的种类。

## Field
图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fnm/3.png">

### FieldName
&emsp;&emsp;该字段描述的是域名，例如图1中的"author"、"content"、"abc"都是FieldName。

### FieldNumber
&emsp;&emsp;域的编号，根据处理域的先后顺序，每个域都会获得一个从0开始递增的域的编号。

### FieldBits
&emsp;&emsp;该字段是一个组合值，它用来描述当前域是否有以下的属性：

- 是否存储词向量(termVector)：0x1，词向量的介绍可以看这里[索引文件之tvx&&tvd](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0429/56.html)
- 是否忽略域的norm值：0x2，用于域的打分的norm值的介绍可以看这里[索引文件之nvd&&nvm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0305/39.html)
- 是否带有payload：0x4，payload的介绍可以看这里[索引文件之pos&&pay](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0324/41.html)
- 该域是否为软删除域(soft delete field)：0x8，这个概念在后面的文档中会介绍

&emsp;&emsp;在图1中，域"content"的FieldBits的值为 (0x1 | 0x2 | 0x4) = 0x0111。

### IndexOptions
&emsp;&emsp;该字段描述了当前域的索引选项(IndexOptions)，IndexOptions有以下值，每个选项的含义在[两阶段生成索引文件之第一阶段](https://www.amazingkoala.com.cn/Lucene/Index/2019/0521/61.html)已介绍，不赘述：

- 0：NONE
- 1：DOCS
- 2：DOCS_AND_FREQS
- 3：DOCS_AND_FREQS_AND_POSITIONS
- 4：DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS

### DocValuesBits
&emsp;&emsp;该字段占用一个字节，其中高4个bit用来描述是否记录norm，低4个bit用来描述DocValues类型，DocValues的类型包括以下类型，在[DocValues](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0218/33.html)对每一种都已介绍，不赘述：

- 0：NONE
- 1：NUMERIC
- 2：BINARY
- 3：SORTED
- 4：SORTED_SET
- 5：SORTED_NUMERIC

### DocValuesGen
&emsp;&emsp;该字段描述了DocValues类型的域的更新状态，比如我们调用IndexWriter.updateDocValues(...)方法后，那么DocValuesGen的值会变更，这里不展开介绍，在介绍IndexWriter时会详细介绍。

### Attributes
&emsp;&emsp;该字段描述了存储当前域的索引文件的格式(format)，比如说当前是一个DocValues的域，那么Attributes的字段会有下面的值：

- PerFieldDocValuesFormat.format：Lucene70

&emsp;&emsp;表示使用[Lucene70](http://lucene.apache.org/core/7_0_0/core/org/apache/lucene/codecs/lucene70/package-summary.html)这种格式来生成[索引文件.dvd、.dvm](https://www.amazingkoala.com.cn/Lucene/DocValues/2019/0218/33.html)。

### DimensionCount
&emsp;&emsp;该字段描述的是如果域为点数据类型，那么DimensionCount的值为点数据的维度，点数据以及维度的概念在[Bkd-Tree](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0422/52.html)以及[索引文件之dim&&dii](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0424/53.html)介绍不赘述，在图1中，IntPoint域即为点数据域，DimensionCount的值为3，因为有3，5，9共三个值。

### DimensionNumBytes
&emsp;&emsp;该字段描述的是每一个维度占用的字节个数（数值类型被编码为多个字节），同样已经在前面的文章中介绍了。


# fnm文件的总数据结构


图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fnm/4.png">

[点击下载](http://www.amazingkoala.com.cn/attachment/Lucene/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6/fnm.zip)Markdown文档

