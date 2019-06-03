## DocValues

在搜索引擎中，我们通常都是对域名(field)构建倒排索引(inverted index)，实现了域值(Values)到文档(document)的映射，而DocValues则是构建了一个正向索引，实现文档到域值的映射。下面是官方给出的DocValues的介绍https://wiki.apache.org/solr/DocValues=
#### What docvalues are:
1. NRT-compatible: These are per-segment datastructures built at index-time and designed to be efficient for the use case where data is changing rapidly.
2. Basic query/filter support: You can do basic term, range, etc queries on docvalues fields without also indexing them, but these are constant-score only and typically slower. If you care about performance and scoring, index the field too.
3. Better compression than fieldcache: Docvalues fields compress better than fieldcache, and "insanity" is impossible.
4. Able to store data outside of heap memory: You can specify a different docValuesFormat on the fieldType (docValuesFormat="Disk") to only load minimal data on the heap, keeping other data structures on disk.
#### What docvalues are not:
1. Not a replacement for stored fields: These are unrelated to stored fields in every way and instead datastructures for search (sort/facet/group/join/scoring).
2. Not a huge improvement for a static index: If you have a completely static index, docvalues won't seem very interesting to you. On the other hand if you are fighting the fieldcache, read on.
3. Not for the risk-averse: The integration with Solr is very new and probably still has some exciting bugs!
#### DocValues的类型
DocValues目前主要有五种类型，随后的博客中会一一详细介绍
1. [SORTED_SET](http://www.amazingkoala.com.cn/Lucene/DocValues/2019/0412/48.html)
2. [SORTED_NUMERIC ](http://www.amazingkoala.com.cn/Lucene/DocValues/2019/0410/47.html)
3. [NUMERIC](http://www.amazingkoala.com.cn/Lucene/DocValues/2019/0409/46.html)
4. [SORTED](http://www.amazingkoala.com.cn/Lucene/DocValues/2019/0219/34.html)
5. [BINARY](http://www.amazingkoala.com.cn/Lucene/DocValues/2019/0412/49.html)