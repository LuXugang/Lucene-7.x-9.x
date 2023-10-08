---
title: FST（二）（Lucene 8.4.0）
date: 2020-10-09 00:00:00
tags: [encode, decode,util,fst]
categories:
- Lucene
- yasuocunchu
---

# [FST（二）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/)（Lucene 8.4.0）

&emsp;&emsp;在文章[FST（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)（**必须先阅读该篇文章**）中我们通过一个例子，简单的描述了Lucene是如何使用一个字节数组current\[ ]存储FST信息的，为了能更好的理解读取过程，我们需要另外给出例子（差别在于把"mop"改成了"mo"），输入数据以及对应FST的信息如下。

```text
String[] inputValues = {"mo", "moth", "pop", "star", "stop", "top"};
long[] outputValues = {100, 91, 72, 83, 54, 55};
```

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/FST（二）/1.png">

&emsp;&emsp;如果用节点跟边的关系来描述图1中的FST信息见下图：

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/FST（二）/2.png">

&emsp;&emsp;由图1可以看出FST的两个特性，相同前缀存储和相同后缀存储:

- 相同前缀存储：
  - mo、moth的相同前缀"mo"
  - stop、star的相同前缀"st"
- 相同后缀存储：
  - pop、top的相同后缀"op"
  - pop、top、stop的相同后缀"p"

## 读取FST

&emsp;&emsp;在文章[FST（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)中我们说到，对于某个term的每一个字符对应的FST信息将以一个四元组信息（**至少包含label跟flag**）被写入到current[ ]数组中，即index、output、label、flag，其中flag跟index用于找出这个term的每一个字符在current[ ]数组中的起始读取位置以及读取区间，label是这个字符的ASCII，output则是这个term的完整或部分附加值，当获取了所有的字符后就能获得完整的附加值。例如"mo"这个term，它包含2个字符，这几个字符在图1的数组current[ ]数组中的数据区间如下表所示：

表一：

| 字符 | 下标值区间 |
| :--: | :--------: |
|  m   |  [35, 38]  |
|  o   |   [5, 7]   |

&emsp;&emsp;接着我们根据顺序读取跟随机读取两种方式来展开介绍，在此之前我们先介绍下flag：

表二：

|           Flag           | Value |                         Description                          |
| :----------------------: | :---: | :----------------------------------------------------------: |
|      BIT_FINAL_ARC       |   1   |            arc对应的label是某个term的最后一个字符            |
|       BIT_LAST_ARC       |   2   | arc是Node节点中的最后一个Arc，上文中我们说到一个UnCompiledNode状态的Node可以包含多个arc |
|     BIT_TARGET_NEXT      |   4   | 上一个由状态UnCompiledNode转为CompiledNode状态的Node是当前arc的target节点, 它实际是用来描述当前的arc中的label不是输入值的最后一个字符，例如"mop"中，“m”、"o"就不是输入值mop的最后一个字符 |
|      BIT_STOP_NODE       |   8   |                  arc的target是一个终止节点                   |
|    BIT_ARC_HAS_OUTPUT    |  16   |                  arc有output值(output不为0)                  |
| BIT_ARC_HAS_FINAL_OUTPUT |  32   |            某个term的最后一个字符带有附加值output            |

&emsp;&emsp;表二中的Flag相比较文章[FST（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)，多了BIT_ARC_HAS_FINAL_OUTPUT，在那一篇文章中未直接给出的原因在于，那篇文章中的例子的特殊性使得不会使用到该flag，这也是为什么本篇文章我们需要换一个例子。

&emsp;&emsp;无论哪种读取方式，总是从current[ ]数组中最后一个有效数据，即下标38，的位置开始读取，又因为四元组信息中至少包含flag跟label信息，故直接从最后一个有效数据开始往前读取两个数组元素，获得label的值为"m"、flag的值为16，接着根据flag的值的组合，执行不同的读取逻辑，如下图所示：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/FST（二）/3.png">

### 顺序读取

&emsp;&emsp;顺序读取描述的是从FST中最小的term开始，根据字典序依次读取所有的term，在下文中的介绍将会了解到，整个过程相当于一次深度遍历。

#### 读取"mo"

&emsp;&emsp;根据下标值38对应的数组元素，即flag，该值为16，该值只包含了BIT_ARC_HAS_OUTPUT（16）那么会执行以下的流程：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/FST（二）/4.png">

&emsp;&emsp;图4流程中，结合表二中flag的介绍可以知道，由于不包含BIT_STOP_NODE，说明当前字符不是某个term的最后一个字符；由于不包含BIT_TARGET_NEXT，说明在current[ ]数组中，离当前字符"m"的最近的一个四元组信息（下标值区间为[ 31, 34]）对应的字符不是当前字符"m"的下一个字符，那么此时需要在数组中读取四元组信息中的index，使得能找到下一个字符的起始读取位置，即下标值为7对应的数组元素，它是下一个字符的四元组信息中的flag。

&emsp;&emsp;根据上面的介绍，我们目前获得了字符"m"、附加值91，以及下一个字符对应的flag，那么根据这个flag继续读取。

&emsp;&emsp;由于四元组信息至少包含flag和label、所以我们可以知道目前我们正在处理字符"o"，根据flag的值，即39， 包含了BIT_ARC_HAS_FINAL_OUTPUT（32）、BIT_TARGET_NEXT（4）、BIT_LAST_ARC（2）、BIT_FINAL_ARC（1），那么它对应的流程图如下所示：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/FST（二）/5.png">

&emsp;&emsp;由于包含BIT_TARGET_NEXT，说明在current[ ]数组中，离当前字符"o"的最近的一个四元组信息（下标区间为[3, 4]）对应的字符是当前字符"o"的在某一个term（即"moth"）中的下一个字符，**但是**由于包含BIT_FINAL_ARC，说明当前字符"o"同时是某个term（即"mo"）的最后一个字符，那么我们可以知道存在"mo"以及以"mo"为前缀的term，另外由于包含了BIT_LAST_ARC，意味着不存在其他不以"mo"为前缀的term。

#### 读取"moth"

&emsp;&emsp;由于在读取"mo"的字符"o"时，它对应的flag中包含BIT_TARGET_NEXT，意味着最靠近字符"o"对应的四元组信息的字符就是字符"o"的在某一个term的下一个字符，由图1可见，即下标区间[3, 4]对应的字符"t"，它对应的四元组信息中的flag值为6，包含了BIT_TARGET_NEXT（4）、BIT_LAST_ARC（2），对应的流程图同图5。

&emsp;&emsp;由于包含了BIT_TARGET_NEXT，并且不包含BIT_FINAL_ARC，那么说明"t"不是某个term的最后一个字符，并且下一个字符为离当前字符"t"的最近的一个四元组信息（下标区间为[1, 2]）对应的字符，即字符"h"。字符"h"对应的四元组信息中的flag值为15，包含了BIT_STOP_NODE（8）、BIT_TARGET_NEXT（4）、BIT_LAST_ARC（2）、BIT_FINAL_ARC（1），对应的流程图如下：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/utils/FST（二）/6.png">

&emsp;&emsp;由于包含了BIT_STOP_NODE，说明没有下一个字符，此时就获得了完整的term，即"moth"’。

&emsp;&emsp;**字符"h"是最后一个字符了，为什么还有BIT_TARGET_NEXT**

&emsp;&emsp;如果你看过文章[FST（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/FST（一）)就会知道，它的target是-1，表示没有合法的下一个字符了。

&emsp;&emsp;通过"mo"、"moth"的介绍已经可以了解读取FST的逻辑，对于BIT_LAST_ARC还需要一点补充，当不包含BIT_LAST_ARC时，说明结点中包含多个Arc，那么在源码中会保留该节点前的字符信息，使得不用重复读取，即实现了深度遍历的逻辑。以图2为例，当读取"star"的字符"a"时，就会知道节点9还有其他的arc，那么此时会保留节点9之前的信息，即字符"s"、"t"的信息，使得读取完"star"后，再读取"stop"时不用再次读取"s"、"t"的信息。

### 随机读取

&emsp;&emsp;随机读取描述的是给定一个term，从FST信息中读取该term的附加值，其基本过程为在每一个节点中进行线性扫描（Linear scan），直到term中每一个字符都能被找到，例如我们给出的输入为"stbae"，结合图2，先在节点1中尝试找到字符"s"，随后在节点8中尝试找到字符"t"，最后在节点9中尝试找到字符"b"，由于节点9中不存在字符"b"对应的arc，那么停止查找并返回，即FST中不存在该term。

## 结语

&emsp;&emsp;本文简单的介绍了FST信息的基本读取过程，在后面的文章中我们将继续介绍Lucene中生成FST的一些其他特性的使用以及优化读取的内容。

[点击](http://www.amazingkoala.com.cn/attachment/Lucene/utils/FST/FST（二）.zip)下载附件



