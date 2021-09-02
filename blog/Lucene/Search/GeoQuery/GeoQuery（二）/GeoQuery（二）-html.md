# [GeoQuery（二）](https://www.amazingkoala.com.cn/Lucene/Search/)（Lucene 8.8.0）

&emsp;&emsp;在上一篇文章[GeoQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search/2021/0817/198.html)中，我们基于下面的例子介绍了在GeoHash编码在Elasticsearch中的部分实现，我们继续介绍其剩余内容。

```java
latitude：32
longitude：50
```

## base32编码

&emsp;&emsp;在前面的步骤中，先将经纬度的值**量化**为两个int类型的数值，随后在**交叉编码**后，将两个int类型的数值用一个long类型的表示。为了便于介绍，我们称这个long类型的值为interleave。最后，interleave在经过base32编码后，我们就能获得GeoHash编码值。

&emsp;&emsp;同样的我们根据Elasticsearch中的源码来介绍其过程。由于在es中地理位置的精度等级为12（Geohash类中的PRECISION变量定义，如图1所示），加上base32核心处理方式是将5个bit用一个字符（char）描述，故意味着interleave的高60个bit是有效的，同时低4个bit可以用来描述其精度值。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（二）/1.png">

### 低4个bit

&emsp;&emsp;上一篇文章中我们获得的interleave的值如下所示：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（二）/2.png">

&emsp;&emsp;当低4个bit用来描述精度12（0b<font color=Red>1100</font>）后，interleave的值如下所示：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（二）/3.png">

### 高60个bit

&emsp;&emsp;接着就可以对高60个bit进行base32编码了。总体流程为从这60个bit的末尾开始，每次取出5个bit，其对应的十进制值作为**编码表**的下标值，在**编码表**中找到对应的字符，最终生成一个包含12个字符的字符串。如下所示：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（二）/4.png">

&emsp;&emsp;从图4中可以看出，我们首先取出5个bit，以**10110**为例，它对应的十进制的值为22，随后将22作为编码表BASE_32的下标值，取出数组元素q，把该值写入到GeoHash编码中。为了图片的整洁性，故图4中只描述了将60个bit的末尾15个bit进行编码的过程。当所有的bit都处理结束后，其最终的GeoHash编码如下所示：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（二）/5.png">

**为什么介绍GeoHash编码**

&emsp;&emsp;在Elasticsearch的文档中我们可以知道，以[geo_bounding_box query](https://www.elastic.co/guide/en/elasticsearch/reference/7.13/query-dsl-geo-bounding-box-query.html)为例，它支持提供GeoHash编码跟经纬度进行地理位置查询，如下所示：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（二）/6.png">

## 结语

&emsp;&emsp;下一篇文章将正式开始介绍Elasticsearch中提供的Geo Queries。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/GeoQuery/GeoQuery（二）.zip)下载附件

