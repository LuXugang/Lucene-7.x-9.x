# [PackedInts（二）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/)

&emsp;&emsp;本文承接[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)，继续介绍剩余的内容。

## 压缩实现

&emsp;&emsp;在上一篇文章中，我们介绍了Lucene 7.5.0中PackedInts提供的几种压缩实现，如下所示：

表1：

<table>
 <col width=87 style='width:65pt'>
 <col width=116 style='mso-width-source:userset;mso-width-alt:3712;width:87pt'>
 <col width=132 style='mso-width-source:userset;mso-width-alt:4224;width:99pt'>
 <col width=176 style='mso-width-source:userset;mso-width-alt:5632;width:132pt'>
 <tr height=21 style='height:16.0pt'>
  <td height=21 width=87 style='height:16.0pt;width:65pt'>数据分布</td>
  <td width=116 style='width:87pt'>是否有填充bit</td>
  <td width=132 style='width:99pt'>是否单block单值</td>
  <td width=176 style='width:132pt'>实现类</td>
 </tr>
 <tr height=101 style='mso-height-source:userset;height:76.0pt'>
  <td rowspan=2 height=418 class=xl65 style='height:314.0pt'>一个block</td>
  <td class=xl65>否</td>
  <td class=xl65>是</td>
  <td class=xl66 width=176 style='width:132pt'>Direct8<br>
    Direct16<br>
    Direct32<br>
    Direct64</td>
 </tr>
 <tr height=317 style='height:238.0pt'>
  <td height=317 class=xl65 style='height:238.0pt'>是</td>
  <td class=xl65>否</td>
  <td class=xl66 width=176 style='width:132pt'>Packed64SingleBlock1<br>
  Packed64SingleBlock2<br>
    Packed64SingleBlock3<br>
    Packed64SingleBlock4<br>
    Packed64SingleBlock5<br>
    Packed64SingleBlock6<br>
    Packed64SingleBlock7<br>
    Packed64SingleBlock8<br>
    Packed64SingleBlock9<br>
    Packed64SingleBlock10<br>
    Packed64SingleBlock12<br>
    Packed64SingleBlock16<br>
    Packed64SingleBlock21<br>
    Packed64SingleBlock32</td>
 </tr>
 <tr height=21 style='height:16.0pt'>
  <td height=21 class=xl65 style='height:16.0pt'>两个block</td>
  <td class=xl65>否</td>
  <td class=xl65>否</td>
  <td class=xl65>Packed64</td>
 </tr>
 <tr class=xl65 height=45 style='height:34.0pt'>
  <td height=45 class=xl65 style='height:34.0pt'>三个block</td>
  <td class=xl65>否</td>
  <td class=xl65>-</td>
  <td class=xl66 width=176 style='width:132pt'>Packed8ThreeBlocks<br>
    Packed16ThreeBlocks</td>
</table>

**我们接着介绍如何选择这些压缩实现**:

&emsp;&emsp;在源码中Lucene会根据使用者提供的三个参数来选择其中一种压缩实现，即PackedInts类中的[getMutable(int valueCount, int bitsPerValue,  float acceptableOverheadRatio)](https://github.com/LuXugang/Lucene-7.5.0/blob/master/solr-7.5.0/lucene/core/src/java/org/apache/lucene/util/packed/PackedInts.java)方法，参数如下所示：

- valueCount：描述待处理的数据集的数量
- bitsPerValue：描述待处理的数据集中的最大值，它的有效数据占用的bit个数
- acceptableOverheadRatio：描述可接受的开销

**什么是可接受的开销acceptableOverheadRatio**：

- bitsPerValue描述了每一个数值占用的bit位个数，acceptableOverheadRatio则是每一个数值额外的空间开销的比例，允许使用比bitsPerValue更多的bit位，我们称之为maxBitsPerValue，来存储每一个数值，计算公式如下所示：

```java
    int maxBitsPerValue = bitsPerValue + (int)(bitsPerValue * acceptableOverheadRatio)
```

&emsp;&emsp;例如我们有以下的数据集：

数组一：

```java
    long[] values = {3, 8, 7, 12, 18};
```

&emsp;&emsp;该数组的bitsPerValue为5，如果此时acceptableOverheadRatio的值为7，那么maxBitsPerValue = 5 + 5\*7 = 40，即允许使用40个bit位来存储数组一。

&emsp;&emsp;当然Lucene并不会真正的使用40个bit来存储一个数值，**maxBitsPerValue只是用来描述使用者可接受的额外开销的程度**。

**为什么要使用acceptableOverheadRatio**：

&emsp;&emsp;**使得在使用者可接受的额外开销前提下，尽量使用读写性能最好的压缩来处理**，我们先看下源码中的部分代码截图：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/压缩存储/PackedInts/PackedInts（二）/1.png">

&emsp;&emsp;先说明下上图的一些内容，actualBitsPerValues的值在后面的逻辑中用来选择对应的压缩实现，actualBitsPerValues与压缩实现的对应关系如下：

- 8：Direct8
- 16：Direct16
- 32：Direct32
- 64：Direct64
- 24：Packed8ThreeBlocks
- 48：Packed16ThreeBlocks
- 随后先考虑是否能用Packed64SingleBlock\*（红框表示），最后才考虑使用Packed64

&emsp;&emsp;在第250行的if语句判断中，如果bitsPerValues的值小于等于8，并且maxBitsPerValue大于等于8，那么就使用Direct8来处理，在文章[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)中我们知道，Direct\*的压缩实现是读写性能最好的，可以看出来acceptableOverheadRatio是空间换时间的设计思想，并且压缩实现的选择优先级如下所示：

```java
    Direct* > Packed*ThreeBlocks > Packed64SingleBlock* > Packed64
```

**acceptableOverheadRatio的取值范围是什么**：

&emsp;&emsp;Lucene提供了以下几种取值：

表2：

| acceptableOverheadRatio |                         源码中的描述                         |
| :---------------------: | :----------------------------------------------------------: |
|            7            | At most 700% memory overhead, always select a direct implementation. |
|           0.5           | At most 50% memory overhead, always select a reasonably fast implementation |
|          0.25           |                 At most 25% memory overhead                  |
|            0            | No memory overhead at all, but the returned implementation may be slow. |

**acceptableOverheadRatio的值为7**

&emsp;&emsp;如果acceptableOverheadRatio的值为7，那么不管bitsPerValue是区间[1, 64]中的哪一个值，总是会选择Direct\*压缩实现。例如bitsPerValue的值为1，那么maxBitsPerValue = 1 + 1\*7 = 8，那么根据图1中第250行的判断，就会使用Direct8来处理，意味着每一个数值使用8个bit位存储，由于每一个数值的有效数据的bit位个数为1，那么每个数值的额外开销为7个bit，即表2中描述的At most 700% memory overhead。

**acceptableOverheadRatio的值为0.5**

&emsp;&emsp;如果acceptableOverheadRatio的值为0.5，那么总能保证选择除了Packed64的其他任意一个压缩实现，它们是比较快（reasonably fast）的实现。

**acceptableOverheadRatio的值为0.25**

&emsp;&emsp;相对acceptableOverheadRatio的值为0的情况获得更多的非Packed64的压缩实现。

**acceptableOverheadRatio的值为0**

&emsp;&emsp;没有任何额外的空间开销，虽然读写性能慢，但是因为使用了固定位数按位存储，并且没有填充bit（见[PackedInts（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/1217/118.html)），所以有较高的压缩率。

### Packed64的实现

&emsp;&emsp;表4中的所有压缩实现，除了Packed64，其他的实现逻辑由于过于简单就不通过文章介绍了，而Packed64的实现核心是BulkOperation，BulkOperation中根据bitsPerValue从1到64的不同取值一共有64种不同的逻辑，但他们的实现原理是类似的，故感兴趣的同学可以看文章[BulkOperationPacked](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0213/31.html)来了解其中的一个实现。

## 结语

&emsp;&emsp;无

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/%E5%8E%8B%E7%BC%A9%E5%AD%98%E5%82%A8/PackedInts（二）.zip)下载附件


