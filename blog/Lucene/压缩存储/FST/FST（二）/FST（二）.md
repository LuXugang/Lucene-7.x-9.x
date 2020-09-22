# [FST（二）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/)（Lucene 8.4.0）

&emsp;&emsp;在文章[FST（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/35.html)中我们基于Lucene 7.5.0并通过一个例子，简单的描述了Lucene是如何使用一个字节数组current\[ ]存储FST信息的，从本篇文章开始，我们基于Lucene 8.4.0并结合代码详细的介绍current\[ ]数组的生成以及读取该数组的过程。阅读本篇文章前建议先阅读文章[FST（一）](https://www.amazingkoala.com.cn/Lucene/yasuocunchu/2019/0220/35.html)。

## 生成FST的流程图

