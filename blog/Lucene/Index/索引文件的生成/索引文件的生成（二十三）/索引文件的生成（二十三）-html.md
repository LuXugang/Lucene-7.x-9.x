# [索引文件的生成（二十三）](https://www.amazingkoala.com.cn/Lucene/Index/)（Lucene 8.6.0）

&emsp;&emsp;从本篇文章开始介绍用于描述存储域（存储域的概念见文章[索引文件之fdx&&fdt&&fdm](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2020/1013/169.html)）的索引文件.fdx、.fdt、.fdm的生成过程，直接给出流程图：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/1.png">

&emsp;&emsp;从图1中可以看出，生成完整的索引文件.fdx、.fdt、.fdm的过程分布在两个阶段：索引阶段、flush阶段。这也解释了为什么在文章[文档提交之flush（三）](https://www.amazingkoala.com.cn/Lucene/Index/2019/0725/76.html)的图5中，其他索引文件都是"生成"、而索引文件.fdx、.fdt、.fdm则是"更新"，注意的是那篇文章中是基于Lucene 7.5.0，故不存在索引文件.fdm。

## 索引阶段

### 处理存储域的域值信息

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/2.png">

&emsp;&emsp;图2中`Document`指定是索引阶段的一篇待处理的文档，我们集合一个例子来加以介绍：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/3.png">

&emsp;&emsp;当开始处理一篇文档（Document）时，我们需要记录对存储域的域值，图1的例子中有两篇文档，在执行了流程点`处理存储域的域值`之后，域值信息将被写入到一个字节数组bufferedDocs[ ]数组（在源码中bufferedDocs其实是一个对象，它用来将数据写入到字节数组buffer[ ]中，为了便于介绍，所以我们直接认为bufferedDocs是一个字节数组）中，域值信息中包含两类信息：

- FieldNumAndType：该信息是**域的编号**跟**域值的值类型**的long类型的组合值，组合公式为：$FieldNumAndType = (域的编号 << 3) | 域值的值类型$，也就是说long类型的FieldNumAndType，低3位用来存储域值的值类型，高61位用来存储域的编号
  - 域的编号是一个从开0开始的递增值，每种域名有唯一的一个编号，根据域名来获得一个域的编号，例如图3中域名”content“是第一个被处理的域名，所以该域的编号为0，同理域名”attachment“、"author"的编号分别为1、2。
  - 域值的值类型共有以下几种，例如图3中第54行的域值的值类型是STRING类型，第56行的域值的值类型是NUMERIC_INT类型
    - STRING：固定值：0x00，域值为String类型
    - BYTE_ARR：固定值：0x01，域值为字节类型
    - NUMERIC_INT：固定值：0x02，域值为int类型
    - NUMERIC_FLOAT：固定值：0x03，域值为float类型
    - NUMERIC_LONG：固定值：0x04，域值为longt类型
    - NUMERIC_DOUBLE：固定值：0x05，域值为double类型
  - 以图3中第56行的域为例，域名"author"的域的编号为2，域值"3"的值类型为NUMERIC_INT，那么组合后$FieldNumAndType = 2 << 3 | 2 = 18$
- Value：该信息包含了域值被编码成字节后的值以及占用的字节数量length

&emsp;&emsp;对于图3的例子，这两篇文档中存储域的域值写入到bufferedDoc[ ]数组后，如下所示：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/4.png">

&emsp;&emsp;图3中文档0有四个域信息，但是域名"attachment"的域的属性为"STORE.NO"，那么域值就不会别写入到bufferedDoc[ ]数组中，这会导致在搜索阶段，当文档0满足某个查询条件后，我们无法获得文档0中域名"attachment"的域值。

&emsp;&emsp;bufferedDoc[ ]数组中的内容将被写入到索引文件.fdt中：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/5.png">

&emsp;&emsp;图5中，每个Doc字段就描述了一篇文档的所有域存储域的域值信息。

### 增量统计存储域的信息

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/6.png">

&emsp;&emsp;在生成一个chunk之前，需要增量统计存储域的信息，满足生成一个chunk的条件后，存储域的信息将被写入到索引文件.fdt中。

&emsp;&emsp;在这个流程点，需要增量的记录下面的数据：

- numBufferedDocs：该值是一个从0开始递增的值，每次处理一个文档（文档中可能不包含存储域），该值+1，它描述了一篇文档的的段内文档号，同时该值也描述了当前生成一个chunk前当前已经处理的文档数量，该信息在下文中中将会用于流程点`是否生成一个chunk`
- numStoredFields[ ]数组：该数组的下标值是numBufferedDocs，数组元素描述的是每篇文档中存储域的数量，例如图3中的文档0，它就包含了3个存储域
- endOffsets[ ]数组：该数组的下标值是numBufferedDocs，数组元素是一个索引值，该值作为bufferedDocs[ ]数组的下标值，用来获取某篇文档的最后一个存储域的域值信息在bufferedDocs[ ]数组中的结束位置

&emsp;&emsp;为了能更好的描述这些信息，我们需要新给一个例子：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/7.png">

&emsp;&emsp;图7中要注意的是，文档1中不包含存储域，处理完这三篇文档后，收集到的存储域信息如下所示：

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/8.png">

&emsp;&emsp;图8中，由于文档1不包含存储域，所以在numStoredFields[ ]数组中，下标值为1的数组元素为0，另外在endOffsets[ ]数组中，文档0的存储域信息存储为下标值1的数组元素，即11，描述了文档0中最后一个存储域的域值信息在bufferedDocs[ ]数组中的结束位置。

&emsp;&emsp;另外文档1中不包含存储域，为什么另它对应在endOffsets[ ]中的数组元素跟文档0是一致以及这些数组如何配合使用的介绍将在随后的介绍索引文件fdx&&fdt&&fdm的读取的文章中再详细展开。

&emsp;&emsp;存储域的信息对应在索引文件.fdt中的字段如下所示：

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/index/索引文件的生成/索引文件的生成（二十三）/9.png">

## 结语

&emsp;&emsp;基于篇幅，剩余的内容将在下一篇文章中展开。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Index/索引文件的生成/索引文件的生成（二十三）/索引文件的生成（二十三）.zip)下载附件
















