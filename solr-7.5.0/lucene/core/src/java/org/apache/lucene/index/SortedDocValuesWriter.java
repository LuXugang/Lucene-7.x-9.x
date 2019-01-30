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
package org.apache.lucene.index;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.BytesRefHash.DirectBytesStartArray;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;
import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_SIZE;

/** Buffers up pending byte[] per doc, deref and sorting via
 *  int ord, then flushes when segment flushes. */
// 此类是对同一种域名进行操作，这些域可能在不同的document中，域值也可能不同
class SortedDocValuesWriter extends DocValuesWriter {
  final BytesRefHash hash;
    // 这个对象中的pending数组记录了所有document包含的termID(非去重的)
    // 例如 文档0包含的termID：0 ，文档1包含的termID：1, 文档2包含的termId：0，文档3包含的termId：1,那么pending对象中的pending数组为：[0,1,0,1]
    private PackedLongValues.Builder pending;
    // 统计一个域在多少个document中出现，在不同的document中的域值可能不同
  private DocsWithFieldSet docsWithField;
  private final Counter iwBytesUsed;
  private long bytesUsed; // this currently only tracks differences in 'pending'
  private final FieldInfo fieldInfo;
  // 记录上一次处理的文档号
  private int lastDocID = -1;

  private PackedLongValues finalOrds;
  private int[] finalSortedValues;
  private int[] finalOrdMap;

  public SortedDocValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
    this.fieldInfo = fieldInfo;
    this.iwBytesUsed = iwBytesUsed;
    hash = new BytesRefHash(
        new ByteBlockPool(
            new ByteBlockPool.DirectTrackingAllocator(iwBytesUsed)),
            BytesRefHash.DEFAULT_CAPACITY,
            new DirectBytesStartArray(BytesRefHash.DEFAULT_CAPACITY, iwBytesUsed));
    pending = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
    docsWithField = new DocsWithFieldSet();
    bytesUsed = pending.ramBytesUsed() + docsWithField.ramBytesUsed();
    iwBytesUsed.addAndGet(bytesUsed);
  }

  public void addValue(int docID, BytesRef value) {
    // 当前处理的文档号是否跟上一个处理的文档号是否一样，如果一样那么抛出异常
    // 即一篇document中，每一个SortedSetDocValuesField域名只能有一个域值
    if (docID <= lastDocID) {
      throw new IllegalArgumentException("DocValuesField \"" + fieldInfo.name + "\" appears more than once in this document (only one value is allowed per field)");
    }
    if (value == null) {
      throw new IllegalArgumentException("field \"" + fieldInfo.name + "\": null value not allowed");
    }
    if (value.length > (BYTE_BLOCK_SIZE - 2)) {
      throw new IllegalArgumentException("DocValuesField \"" + fieldInfo.name + "\" is too large, must be <= " + (BYTE_BLOCK_SIZE - 2));
    }

    addOneValue(value);
    docsWithField.add(docID);

    lastDocID = docID;
  }

  @Override
  public void finish(int maxDoc) {
    updateBytesUsed();
  }

  private void addOneValue(BytesRef value) {
        // termId就是数组byteStart的下标值，这个下标值对应的数组元素是二维数组bytes[][]的起始位置，这个二维数组存放了域值
        // 同时termID是一个递增的值
        int termID = hash.add(value);
        // if语句为true：说明域值value已经之前处理过了
    if (termID < 0) {
      termID = -termID-1;
    } else {
      // reserve additional space for each unique value:
      // 1. when indexing, when hash is 50% full, rehash() suddenly needs 2*size ints.
      //    TODO: can this same OOM happen in THPF?
      // 2. when flushing, we need 1 int per value (slot in the ordMap).
      iwBytesUsed.addAndGet(2 * Integer.BYTES);
    }
    
    pending.add(termID);
    updateBytesUsed();
  }
  
  private void updateBytesUsed() {
    final long newBytesUsed = pending.ramBytesUsed() + docsWithField.ramBytesUsed();
    iwBytesUsed.addAndGet(newBytesUsed - bytesUsed);
    bytesUsed = newBytesUsed;
  }

  @Override
  Sorter.DocComparator getDocComparator(int maxDoc, SortField sortField) throws IOException {
    assert sortField.getType().equals(SortField.Type.STRING);
    assert finalSortedValues == null && finalOrdMap == null &&finalOrds == null;
    int valueCount = hash.size();
    finalSortedValues = hash.sort();
    finalOrds = pending.build();
    finalOrdMap = new int[valueCount];
    for (int ord = 0; ord < valueCount; ord++) {
      finalOrdMap[finalSortedValues[ord]] = ord;
    }
    final SortedDocValues docValues =
        new BufferedSortedDocValues(hash, valueCount, finalOrds, finalSortedValues, finalOrdMap,
            docsWithField.iterator());
    return Sorter.getDocComparator(maxDoc, sortField, () -> docValues, () -> null);
  }

  private int[] sortDocValues(int maxDoc, Sorter.DocMap sortMap, SortedDocValues oldValues) throws IOException {
    int[] ords = new int[maxDoc];
    Arrays.fill(ords, -1);
    int docID;
    while ((docID = oldValues.nextDoc()) != NO_MORE_DOCS) {
      int newDocID = sortMap.oldToNew(docID);
      ords[newDocID] = oldValues.ordValue();
    }
    return ords;
  }

  @Override
  public void flush(SegmentWriteState state, Sorter.DocMap sortMap, DocValuesConsumer dvConsumer) throws IOException {
    final int valueCount = hash.size();
    final PackedLongValues ords;
    final int[] sortedValues;
    final int[] ordMap;
    if (finalOrds == null) {
            // 这个方法是对ids[]数组进行排序，因为下标是hash值，所以termID会分散在数组的不同位置
            // 调用这个方法后，所有termID的值都处在数组最前面, 并且按照termID对应的域值排序(重要)
      sortedValues = hash.sort();
      ords = pending.build();
      ordMap = new int[valueCount];
            //trick: 调用结束后，ordMap数组的下标是termID，数组元素，用来描述termID在sortedValues数组中的下标值
            for (int ord = 0; ord < valueCount; ord++) {
        ordMap[sortedValues[ord]] = ord;
      }
    } else {
      sortedValues = finalSortedValues;
      ords = finalOrds;
      ordMap = finalOrdMap;
    }

    final int[] sorted;
    if (sortMap != null) {
      sorted = sortDocValues(state.segmentInfo.maxDoc(), sortMap,
          new BufferedSortedDocValues(hash, valueCount, ords, sortedValues, ordMap, docsWithField.iterator()));
    } else {
      sorted = null;
    }
    dvConsumer.addSortedField(fieldInfo,
                              new EmptyDocValuesProducer() {
                                @Override
                                public SortedDocValues getSorted(FieldInfo fieldInfoIn) {
                                  if (fieldInfoIn != fieldInfo) {
                                    throw new IllegalArgumentException("wrong fieldInfo");
                                  }
                                  final SortedDocValues buf =
                                      new BufferedSortedDocValues(hash, valueCount, ords, sortedValues, ordMap, docsWithField.iterator());
                                  if (sorted == null) {
                                   return buf;
                                  }
                                  return new SortingLeafReader.SortingSortedDocValues(buf, sorted);
                                }
                              });
  }

  private static class BufferedSortedDocValues extends SortedDocValues {
    final BytesRefHash hash;
    final BytesRef scratch = new BytesRef();
    final int[] sortedValues;
    final int[] ordMap;
    final int valueCount;
    private int ord;
    final PackedLongValues.Iterator iter;
    final DocIdSetIterator docsWithField;

    public BufferedSortedDocValues(BytesRefHash hash, int valueCount, PackedLongValues docToOrd, int[] sortedValues, int[] ordMap, DocIdSetIterator docsWithField) {
      this.hash = hash;
      this.valueCount = valueCount;
      this.sortedValues = sortedValues;
      this.iter = docToOrd.iterator();
      this.ordMap = ordMap;
      this.docsWithField = docsWithField;
    }

    @Override
    public int docID() {
      return docsWithField.docID();
    }

    @Override
    public int nextDoc() throws IOException {
        // 获得文档号
      int docID = docsWithField.nextDoc();
      if (docID != NO_MORE_DOCS) {
        // 获得termId
        ord = Math.toIntExact(iter.next());
        // 根据termID获得sortedValues数组的一个下标值
        ord = ordMap[ord];
      }
      return docID;
    }

    @Override
    public int advance(int target) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean advanceExact(int target) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public long cost() {
      return docsWithField.cost();
    }

    @Override
    public int ordValue() {
      return ord;
    }

    @Override
    public BytesRef lookupOrd(int ord) {
      assert ord >= 0 && ord < sortedValues.length;
      assert sortedValues[ord] >= 0 && sortedValues[ord] < sortedValues.length;
      hash.get(sortedValues[ord], scratch);
      return scratch;
    }

    @Override
    public int getValueCount() {
      return valueCount;
    }
  }

  @Override
  DocIdSetIterator getDocIdSet() {
    return docsWithField.iterator();
  }
}
