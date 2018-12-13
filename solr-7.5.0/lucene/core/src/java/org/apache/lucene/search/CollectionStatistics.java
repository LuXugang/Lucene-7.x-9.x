/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.search;

import org.apache.lucene.index.IndexReader; // javadocs
import org.apache.lucene.index.Terms;       // javadocs


/**
 * Contains statistics for a collection (field)
 * @lucene.experimental
 */
 // CollectionStatistics类用来描述一个域的信息
public class CollectionStatistics {
  private final String field;
  // IndexReader中包含的文档总数
  private final long maxDoc;
  // 包含field的域值的文档总数
  private final long docCount;
  // 这个field包含的所有域值的个数(非去重)
  private final long sumTotalTermFreq;
  //
  private final long sumDocFreq;
  
  public CollectionStatistics(String field, long maxDoc, long docCount, long sumTotalTermFreq, long sumDocFreq) {
    assert maxDoc >= 0;
    assert docCount >= -1 && docCount <= maxDoc; // #docs with field must be <= #docs
    assert sumDocFreq == -1 || sumDocFreq >= docCount; // #postings must be >= #docs with field
    assert sumTotalTermFreq == -1 || sumTotalTermFreq >= sumDocFreq; // #positions must be >= #postings
    this.field = field;
    this.maxDoc = maxDoc;
    this.docCount = docCount;
    this.sumTotalTermFreq = sumTotalTermFreq;
    this.sumDocFreq = sumDocFreq;
  }
  
  /** returns the field name */
  // 域名
  public final String field() {
    return field;
  }
  
  /** returns the total number of documents, regardless of 
   * whether they all contain values for this field. 
   * @see IndexReader#maxDoc() */
  // 返回当前IndexReader中的文档总数，即使有些文档不包含field的域值
  public final long maxDoc() {
    return maxDoc;
  }
  
  /** returns the total number of documents that
   * have at least one term for this field. 
   * @see Terms#getDocCount() */
  // 包含field的域值的文档总数
  public final long docCount() {
    return docCount;
  }
  
  /** returns the total number of tokens for this field
   * @see Terms#getSumTotalTermFreq() */
  // 这个域的所有域值的个数（不是去重的)
  public final long sumTotalTermFreq() {
    return sumTotalTermFreq;
  }
  
  /** returns the total number of postings for this field 
   * @see Terms#getSumDocFreq() */
  // 所有域值所在文档的文档总数的和(文档号可能会重复)，比如 包含域值a的文档数为10，包含域值b的文档数为11，那么sumTotalTermFreq的值为21（10 + 11）
  public final long sumDocFreq() {
    return sumDocFreq;
  }
}
