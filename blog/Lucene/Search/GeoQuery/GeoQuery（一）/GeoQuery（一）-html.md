# [GeoQuery（一）](https://www.amazingkoala.com.cn/Lucene/Search/)（Lucene 8.8.0）

&emsp;&emsp;本系列文章将介绍下Elasticsearch中提供的几个[地理查询](https://www.elastic.co/guide/en/elasticsearch/reference/7.13/geo-queries.html)（Geo Query）在Lucene层的相关内容。Elasticsearch 7.13版本中提供了以下的Geo Queries：

- [geo_bounding_box](https://www.elastic.co/guide/en/elasticsearch/reference/7.13/query-dsl-geo-bounding-box-query.html) query
- [geo_distance](https://www.elastic.co/guide/en/elasticsearch/reference/7.13/query-dsl-geo-distance-query.html) query
- [geo_polygon](https://www.elastic.co/guide/en/elasticsearch/reference/7.13/query-dsl-geo-polygon-query.html) query
- [geo_shape](https://www.elastic.co/guide/en/elasticsearch/reference/7.13/query-dsl-geo-shape-query.html) query

## 预备知识

&emsp;&emsp;为了能深入理解GeoQuery，我们需要先介绍下两个预备知识：

- Lucene中点数据的索引与查询
- GeoHash编码

### Lucene中点数据的索引与查询

#### 点数据

&emsp;&emsp;Lucene中，点数据域用于存储数值类型的信息，并且点数据可以是多维的，例如图1中的第48行、49行分别写入了一个int类型的二维点数据跟三维点数据。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/1.png">

&emsp;&emsp;其他类型的点数据如下所示：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/2.png">

&emsp;&emsp;基于篇幅，后续的文章中只会介绍跟上文中的Geo Queries相关的点数据域。


#### 索引（indexing）和搜索（search）点数据

&emsp;&emsp;在文章[索引文件之dim&&dii](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0424/53.html)中介绍了Lucene中点数据对应的索引数据结构，以及在文章[Bkd-Tree](https://www.amazingkoala.com.cn/Lucene/gongjulei/2019/0422/52.html)中通过一个例子简单的介绍了如何对点数据集合进行划分，并用于生成一棵BKD树，以及在系列文章[索引文件的生成（八）~（十四）](https://www.amazingkoala.com.cn/Lucene/Index/2020/0329/128.html)中详细的介绍了生成[索引文件之dim&&dii](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0424/53.html)的过程，最后在系列文章[索引文件的读取（一）~（四）](https://www.amazingkoala.com.cn/Lucene/Search/2020/0427/135.html)中介绍了[索引文件之dim&&dii](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0424/53.html)的读取过程，即点数据的搜索过程。

&emsp;&emsp;如果阅读过上述的文章，那么相信能非常容易理解GeoQuery在Lucene中的实现。

### GeoHash编码

&emsp;&emsp;接着我们将先介绍区域编码的概念，随后介绍下在Elasticsearch中GeoHash编码的实现。

#### 区域编码（domain encode）

&emsp;&emsp;区域编码描述的是使用唯一的编码值来描述平面上的一块区域。对于一个有边界的二维空间，可以在水平方向或者垂直方向对空间进行划分，那么空间将被划分为四块区域。这四块子区域可以用2个bit号来进行编码。如果规定在水平方向划分后，左边的区域用0表示，右边的区域的用1表示；在垂直方向划分后，下面的区域用0表示，上面的区域用1表示，那么这四个区域就可以分别用00、01、11、10进行编码，如下所示：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/3.png">

&emsp;&emsp;按照上述的切分方式，如果我们继续分别对四块区域进行水平方向跟垂直方向的划分，如下所示：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/4.png">

&emsp;&emsp;区域编码常用于计算空间里面点与点之间的距离关系，从这种区域编码方式能发现：拥有相同前缀的长度越长，空间上的位置就越接近，不过反过来就不一定成立了，因为空间中的两个点可能非常接近，但是这两个点所属的区域可能拥有很短的前缀，甚至没有相同前缀。

&emsp;&emsp;图5中，**<font color=black>黑点</font>**跟<font color=Red>红点</font>没有相同的编码前缀，跟<font color=green>绿点</font>有相同的编码前缀，但是**<font color=black>黑点</font>**跟<font color=Red>红点</font>更为接近。

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/5.png">

#### 查找附近的点

&emsp;&emsp;使用区域编码来实现查找附近的点的直观方法就是找出与查询点所属的编码值前缀最长的区域，这些区域中的点都是认为是在查询点附近的。由于存在图5中描述的问题，所以这种方式会遗漏一些附近点（<font color=Red>红点</font>）。

&emsp;&emsp;为了解决这个遗漏问题，我们可以把查询点所属的区域的周围8个区域中的点都找出来就可以了，当然这种粗暴的做法的缺点是可能会获得大量的结果集。为了防止出现大量结果集，那么可以对当前最小区域再进行划分。

&emsp;&emsp;那么问题就转变为如何通过查询点所在的区域编码获取它周围8个区域的编码值了。

##### 计算规则

&emsp;&emsp;以图5中**<font color=black>黑点</font>**所在区域为例，区域编码为<font color=red>0</font><font color=blue>0</font><font color=red>1</font><font color=blue>1</font>。它的水平编码为<font color=red>01</font>，垂直编码为<font color=blue>01</font>，该区域上下左右四个区域的编码值如下所示：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/6.png">

&emsp;&emsp;同理可以计算出查询点所属的区域的左上、左下、右上、右下四个区域编码值。

##### 精度

&emsp;&emsp;由上面的划分方式可以看出，划分次数越多，区域越小，意味着精度越高。精度高一方面描述了区域编码值能表示更少的点，另一方面也为查找附近点能提高更少的结果集。

#### Elastisearch中的GeoHash编码

&emsp;&emsp;Geohash编码指的是将经纬度坐标转换为字符串的编码方式。它同样通过区域编码的处理方式对地理位置进行了划分。由于地理位置可以用经纬度来表示，其中纬度的取值范围为[-90, 90]，经度的取值范围为[-180, 180]，即对一个有边界的二维区域进行区域编码。

##### 编码

&emsp;&emsp;我们先看下Elasticsearch中如何将经纬度值转变为GeoHash编码。

###### 量化

&emsp;&emsp;首先将经纬度两个值分别量化（quantizing）为区间为[0, 2^32 - 1]的值，使得可以用int类型来描述经纬度，其量化公式如下所示：

```java
int latEnc = (int) Math.floor(latitude / (180.0D/(0x1L<<32)))
int lonEnc = (int) Math.floor(longitude / (360.0D/(0x1L<<32)))
```

&emsp;&emsp;比如纬度的取值范围为[-90, 90]，那么维度值-90跟90将分别量化为 0、2^32 - 1。

###### 交叉编码

&emsp;&emsp;将量化后的维度、经度值，即两个32位的int类型的值交叉编码为一个64个bit的long类型的值。在这个64个bit中，奇数位共32个bit为量化后的维度值对应的32个bit，偶数位共32个bit为量化后的经度值对应的32个bit。

&emsp;&emsp;例如有一个坐标值如下所示：

```java
latitude：32
longitude：50
```

&emsp;&emsp;量化后的值为：

```java
int latEnc = 0b10101101_10000010_11011000_00101101
int lonEnc = 0b10100011_10001110_00111000_11100011
```

&emsp;&emsp;最后对latEnc跟lonEnc进行交叉编码，其处理过程如下图所示：

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/7.png">

&emsp;&emsp;图7中，我们先将int类型的latEnc中的32个bit塞到一个long类型的v1中。

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/8.png">

&emsp;&emsp;同图7的处理方式一样，将int类型的lonEnc中的32个bit塞到一个long类型的v2中。

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/9.png">

&emsp;&emsp;图9中先将v2左移一位，然后 v2 跟v1执行或操作获得一个long类型的值interleave。

&emsp;&emsp;图7~图8的处理过程对应Elasticsearch中的源码BitUtil类中的interleave方法如下所示：

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/GeoQuery/GeoQuery（一）/10.png">

###### base32编码

&emsp;&emsp;在上文中我们获得了interleave的值后，需要对其进行base32编码，在此之后就获得了GeoHash编码。基于篇幅，该内容将在下一篇文章中展开。

## 结语

&emsp;&emsp;Elasticsearch中GeoHash编码原理基于莫顿编码，感兴趣的同学可以深入理解下：http://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN 。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/Search/GeoQuery/GeoQuery（一）.zip)下载附件

