---
title: QueryCache（Lucene 9.11.0）
date: 2024-08-29 00:00:00
tags: [cache, querycache, lru]
categories:
- Lucene
- Search
---


QueryCache是一个用于缓存查询结果的组件，旨在提高重复查询的性能。它通过在**段级别**缓存查询结果，避免了重复计算，从而减少查询响应时间和系统资源消耗。

## 数据结构

在介绍QueryCache的缓存逻辑之前，我们有必要先介绍下几个关键的数据结构。

### CacheAndCount

QueryCache仅仅缓存命中的文档ID集合以及文档总数。在源码中，使用`CacheAndCount`描述缓存的内容：

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/1.png">

**通过文档ID集合可以计算出文档总数，为什么还要额外记录count？**

在OLAP系统中，计算命中的数量是一个常用的指标。Lucene在[LUCENE-9620](https://github.com/apache/lucene/pull/242)新增了count接口，在一些特定的场景中，计算命中数量不需要逐个遍历匹配到的文档。比如在`TermQuery`中，可以直接通过[索引文件.tim](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0401/%E7%B4%A2%E5%BC%95%E6%96%87%E4%BB%B6%E4%B9%8Btim&&tip/)中的`docFreq`字段获取，而不需要找到所有满足查询的文档号并且计算出总数。当然使用这个接口有一定的限制，具体内容将在以后的文章中介绍count接口。

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/2.png">

如果只使用了count接口，那么图1中的文档ID集合是无法获取的，因此需要额外的`count`字段来缓存命中数量。

生成`CacheAndCount`的方式见下文中`BulkScorer（可选阅读）`的介绍。

### 段缓存

QueryCache属于段级别的缓存，对于某一个Query，会在每一个段中生成一个缓存，在每一个段中，会使用`LeafCache`保存Query的缓存，其数据结构如下所示：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/3.png">

通过图3中`cache`这个Map的键值对可以很清晰的看出，`cache`描述了在某个段中，所有Query对应的缓存结果，其中`CacheAndCount`在上文中已经介绍。

### 全局缓存

当我们定义`IndexSearcher`时，会使用一个全局缓存来管理所有段的缓存：

图4-1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/4-1.png"  width="800">


这个Map中，`IndexReader.CacheKey`用来表示某一个段的标识，而`LeafCache`为这个段中的所有缓存。

因此在查询阶段，在访问某个段时，先根据这个段的唯一标示`IndexReader.CacheKey`，找到这个段中的缓存`LeafCache`，然后接着根据Query尝试查找在这个段中的缓存。

#### IndexReader.CacheKey

IndexReader.CacheKey作为段的唯一标示，每个段会New一个下面的对象来实现，使用对象作为Key：

图4-2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/4-2.png"  width="500">

#### IdentityHashMap

这里使用IdentityHashMap使得使用对象引用(地址)而不是对象内容（`equal()`）来判断两个键是否相等。

## 何时使用缓存

不是每一次的查询都会使用缓存，详细内容见在文章[LRUQueryCache](https://www.amazingkoala.com.cn/Lucene/Search/2019/0506/LRUQueryCache/)中的介绍。

## 缓存限制

源码中定义了两个参数来平衡缓存的性能和资源使用：

- maxSize：缓存中允许存储的最大查询数量，设定为查询缓存的条目数上限，默认值为 `1000`。该参数限制了缓存中存储的查询条目的总数，以防止缓存过度膨胀，影响内存利用效率
- maxRamBytesUsed：缓存中所占用的最大内存字节数限制。该参数限制了缓存使用的最大内存量，确保缓存不会占用过多的 JVM 堆内存。其具体计算方法如下

```java
maxRamBytesUsed = Math.min(1L << 25, Runtime.getRuntime().maxMemory() / 20);
```

也就是默认允许最多1000条不同Query的缓存以及最多32M的内存。

## 缓存统计数据

QueryCache中增加以下的统计值以及一些接口，有助于理解缓存的使用情况以及其对系统性能的影响，从而帮助用户进行缓存策略的优化和调优。

- hitCount：记录缓存命中的次数
- missCount：记录缓存未命中的次数
- cacheSize：记录当前缓存中存储的查询结果数量。了解缓存的利用率和是否需要增加或减少缓存的容量
- cacheCount：记录自缓存创建以来，曾经被缓存过的 `CacheAndCount` 总数（无论是否已被移除）帮助理解缓存的使用历史和工作负载特性
- getEvictionCount()：记录缓存中被移除（删除）的缓存数量。频繁的驱逐可能表明缓存空间不足或者缓存策略不适合当前的查询工作负载
- ramBytesUsed：所有段的缓存占用的内存量。该值可以通来判断是否达到缓存限制（即上文中的`maxRamBytesUsed`）

## 缓存逻辑

我们先介绍下触发QueryCache的流程点：

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/5.png"  width="700">

QueryCache在查询流程中并不是一个额外的流程点，它封装了其他的Weight对象，并提供自己的BulkScorer的实现。

### BulkScorer（可选阅读）

在文章[BulkScorer（一）](https://www.amazingkoala.com.cn/Lucene/Search/2023/0707/BulkScorer%EF%BC%88%E4%B8%80%EF%BC%89/)跟[BulkScorer（二）](https://www.amazingkoala.com.cn/Lucene/Search/2023/0724/BulkScorer%EF%BC%88%E4%BA%8C%EF%BC%89/)详细介绍了BulkScorer的内容。可以将BulkScorer最核心的一个功能简单的理解为：它至少包含了一个文档号集合，然后定义了文档号集合读取方式。

对于QueryCache，如果是第一次缓存某个Query，则会基于满足这个Query的文档号集合，使用`DefaultBulkScorer`实现BulkScorer的逻辑，`DefaultBulkScorer`的介绍见文章[BulkScorer（一）](https://www.amazingkoala.com.cn/Lucene/Search/2023/0707/BulkScorer%EF%BC%88%E4%B8%80%EF%BC%89/)。

上文中我们提到缓存的内容用`CacheAndCount`描述，那么`CacheAndCount`是如何获取的呢？

由于QueryCache封装了一个BulkScorer，因此只需要提供一个最简化的收集器Collector，使用这个Collector执行`BulkScorer.score(LeafCollector collector, Bits acceptDocs)`方法就可以获取到满足查询条件的文档号。

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/6.png"  width="900">

### 写入缓存

图7：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/7.png"  width="600">

图6展示的是在写入缓存的流程图，采用的是经典的LRU（Least Recently Used）算法。

#### 准备数据

图8：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/8.png"  width="400">

写入缓存前需要准备三样东西：

- Query：待缓存的Query对象，在上文`LeafCache`中提到，它可以作为一个Key，找到对应的缓存内容`CacheAndCount`
- CacheAndCount：某个Query对应的缓存内容，即文档号以及文档号总数
- IndexReader.CacheHelper：这个概念在下文图10中会展开介绍其作用

#### 调整Query的访问

图9：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/9.png"  width="400">

源码中使用了下面的Map对象来描述LRU中Query的访问先后顺序：

```java
Map<Query, Query> uniqueQueries = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true))
```

拆解分析下这个Map：

- `LinkedHashMap<>(16, 0.75f, true)`是基于访问顺序的LinkedHashMap：在每次访问元素（不论是通过 get 方法读取，还是通过 put 方法插入）时，这个元素都会被移动到链表的末尾。这样，最早被访问的元素会排在最前面，最近被访问的元素会排在最后。

- `Collections.synchronizedMap()`是用于生成线程安全的 Map 的包装器，默认情况下，HashMap及其子类（如 LinkedHas·hMap）不是线程安全的。在多线程环境中访问或修改 Map 时，如果没有同步措施，就可能导致数据不一致或异常。

#### 将Query的缓存添加到当前段的缓存中

图10：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/10.png"  width="400">

该流程尝试从图4-1的`cache`容器中找到当前段内的所有缓存`LeafCache`，如果当前段没有任何缓存，则初始化当前段的缓存，否则将Query的的缓存`CacheAndCount`直接添加到`LeafCahce`中，见图3。

如果需要初始化当前段的缓存，则随后还有一个很重要的步骤：为这个段添加一个回调方法，使得当这个段不在被使用（比如这个段不被任何IndexReader引用时）时清空段中的缓存，目的是释放内存。

##### 清空段中的缓存

需要清除的内容包括：

- 从上文中提到的`ramBytesUsed`中移除这个段中的所有缓存占用的内存。
- 从上文中提到的`cacheSize`中移除这个段中缓存的数量

#### 基于LRU尝试移除一些缓存

图11：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/11.png"  width="300">

##### 步骤一：是否需要移除现有的缓存

首先判断是否需要移除现有的一些缓存，判断条件如下：

```java
 size > maxSize || ramBytesUsed > maxRamBytesUsed
```

- `size`指的是图9中`uniqueQueries`中Key的数量，也就是缓存的Query数量，判断是否达到上文中提到的`maxSize`
- `ramBytesUsed`跟`maxRamBytesUsed`在上文中已经介绍，判断目前缓存占用的内存是否达到阈值。

##### 步骤二：移除缓存

如果需要移除现有的缓存，则从`uniqueQueries`的第一个元素开始依次移除，每移除一个Query对应的缓存就执行下步骤一，判断是否要继续移除，直到不满足步骤一中的判断条件。

注意是，**需要从所有段中移除某一个Query对应的缓存**。


## 缓存过期

Lucene中不存在缓存过期的问题，因为每个段在生成后其内容是不会变的，如果后续的删除/更新操作作用到这个段，那么只有重新打开新的IndexReader以及新的IndexSearch才能感知这些变更，使得会生成新的QueryCache对象。

## 缓存的负面影响

当Query第一次缓存时，也就是满足上文中`何时使用缓存`条件后，可能会导致这一次的Query响应较慢。

例如索引数据在写入时按照某个字段排序，并且查询阶段使用了相同字段的排序规则。在不使用缓存获取TopN时，当**收集器收集到N个文档号**就可以基于[early termination](https://www.amazingkoala.com.cn/Lucene/Search/2023/0626/%E6%AE%B5%E7%9A%84%E5%A4%9A%E7%BA%BF%E7%A8%8B%E6%9F%A5%E8%AF%A2%EF%BC%88%E4%B8%80%EF%BC%89/)提前结束查询。

由于early termination收集器Collector中实现，并且QueryCache替换成了图6中的简易Collector，使得这个Collector需要收集**完整的满足这个Query的文档号集合**。

**但是后续的Query响应速度会快于缓存前以及第一次缓存时的Query，因为同时利用了缓存以及early termination机制**。

上述场景可以看这个[Demo](https://github.com/LuXugang/Lucene-7.x-9.x/blob/master/LuceneDemo9.10.0/src/main/java/org/example/TestLRUCache.java)。执行15次循环查询同一个Query花费的时间如下所示：

图12：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Search/QueryCache/queryCache/12.png" >

图12中前三次查询未满足缓存的条件，未触发缓存，而第4次查询将写入缓存，后续的查询使用了查询。
