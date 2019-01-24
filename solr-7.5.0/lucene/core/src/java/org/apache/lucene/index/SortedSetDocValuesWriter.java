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
import org.apache.lucene.search.SortedSetSelector;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.BytesRefHash.DirectBytesStartArray;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;

import static org.apache.lucene.index.SortedSetDocValues.NO_MORE_ORDS;
import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;
import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_SIZE;

/** Buffers up pending byte[]s per doc, deref and sorting via
 *  int ord, then flushes when segment flushes. */
class SortedSetDocValuesWriter extends DocValuesWriter {
  final BytesRefHash hash;
    // 对象中的一个Long数组，数组中的元素分布是块状相连，块间按照文档号排序，块内按照termID的大小排序
    // 如果有文档1，它的termID有0，1，2；文档2，它的termID是 3，2，那么两篇文档在Long数组中的排列是, [0,1,2,2,3],其中下标0到2是文档1的termID，其他是文档2的termID
    private PackedLongValues.Builder pending; // stream of all termIDs
    // 对象中的一个Long数组，下标是文档号，数组元素是这个文档号的一个域名的域值个数(去重的个数)
    private PackedLongValues.Builder pendingCounts; // termIDs per doc
    // 统计一个域在多少个document中出现，在不同的document中的域值可能不同
    private DocsWithFieldSet docsWithField;
    private final Counter iwBytesUsed;
    private long bytesUsed; // this only tracks differences in 'pending' and 'pendingCounts'
    private final FieldInfo fieldInfo;
    // 记录上一次处理的文档号，跟SortedDocValuesField不同的是，一个document可以有多个域名相同的SortedSetDocValuesField对象
    // 而SortedDocValuesField则不同，当currentDoc的值跟当前的文档号不同，那么会执行finishCurrentDoc()的操作
    private int currentDoc = -1;
    // 存放一篇文档号某个域的所有域值对应的termID(去重的),并且是有序的
    // 这个数组是复用的，处理另一篇文档时候，通过覆盖来复用这个数组
    // 数组中 0~currentUpto范围的元素个数代表了一篇文档中不同域值的个数
    private int currentValues[] = new int[8];
    // 用来一篇文档中包含这个域对应不同域值的个数
    private int currentUpto;
    private int maxCount;

  private PackedLongValues finalOrds;
  private PackedLongValues finalOrdCounts;
  private int[] finalSortedValues;
  private int[] finalOrdMap;


  public SortedSetDocValuesWriter(FieldInfo fieldInfo, Counter iwBytesUsed) {
    this.fieldInfo = fieldInfo;
    this.iwBytesUsed = iwBytesUsed;
    hash = new BytesRefHash(
        new ByteBlockPool(
            new ByteBlockPool.DirectTrackingAllocator(iwBytesUsed)),
            BytesRefHash.DEFAULT_CAPACITY,
            new DirectBytesStartArray(BytesRefHash.DEFAULT_CAPACITY, iwBytesUsed));
    pending = PackedLongValues.packedBuilder(PackedInts.COMPACT);
    pendingCounts = PackedLongValues.deltaPackedBuilder(PackedInts.COMPACT);
    docsWithField = new DocsWithFieldSet();
    bytesUsed = pending.ramBytesUsed() + pendingCounts.ramBytesUsed();
    iwBytesUsed.addAndGet(bytesUsed);
  }

  public void addValue(int docID, BytesRef value) {
    assert docID >= currentDoc;
    if (value == null) {
      throw new IllegalArgumentException("field \"" + fieldInfo.name + "\": null value not allowed");
    }
    if (value.length > (BYTE_BLOCK_SIZE - 2)) {
      throw new IllegalArgumentException("DocValuesField \"" + fieldInfo.name + "\" is too large, must be <= " + (BYTE_BLOCK_SIZE - 2));
    }
        // 当前处理的文档号跟currentDoc值不同，说明上一个document中所有的SortedSetDocValues对象都处理结束了
        if (docID != currentDoc) {
            finishCurrentDoc();
            // 更新currentDoc的值
            currentDoc = docID;
        }

        addOneValue(value);
        updateBytesUsed();
    }

    // finalize currentDoc: this deduplicates the current term ids
    private void finishCurrentDoc() {
        if (currentDoc == -1) {
            return;
        }
        // 数组元素排序
        Arrays.sort(currentValues, 0, currentUpto);
        int lastValue = -1;
        int count = 0;
        for (int i = 0; i < currentUpto; i++) {
            int termID = currentValues[i];
            // if it's not a duplicate
            // 去重操作, 因为数组的元素是有序的，所以只要将当前值跟lastValue比较即可
            if (termID != lastValue) {
                pending.add(termID); // record the term id
                count++;
            }
            lastValue = termID;
        }
        // record the number of unique term ids for this doc
        // 统计这篇文档中同一个域的不同域值的个数(去重)
        pendingCounts.add(count);
        maxCount = Math.max(maxCount, count);
        // 这个值置为0，因为currentValue[]数组是复用的
        currentUpto = 0;
        // 记录这个域在多少个document中出现
        docsWithField.add(currentDoc);
    }

  @Override
  public void finish(int maxDoc) {
    finishCurrentDoc();
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

        if (currentUpto == currentValues.length) {
            currentValues = ArrayUtil.grow(currentValues, currentValues.length+1);
            iwBytesUsed.addAndGet((currentValues.length - currentUpto) * Integer.BYTES);
        }

        currentValues[currentUpto] = termID;
        currentUpto++;
    }

    private void updateBytesUsed() {
        final long newBytesUsed = pending.ramBytesUsed() + pendingCounts.ramBytesUsed();
        iwBytesUsed.addAndGet(newBytesUsed - bytesUsed);
        bytesUsed = newBytesUsed;
    }

  private long[][] sortDocValues(int maxDoc, Sorter.DocMap sortMap, SortedSetDocValues oldValues) throws IOException {
    long[][] ords = new long[maxDoc][];
    int docID;
    while ((docID = oldValues.nextDoc()) != NO_MORE_DOCS) {
      int newDocID = sortMap.oldToNew(docID);
      long[] docOrds = new long[1];
      int upto = 0;
      while (true) {
        long ord = oldValues.nextOrd();
        if (ord == NO_MORE_ORDS) {
          break;
        }
        if (upto == docOrds.length) {
          docOrds = ArrayUtil.grow(docOrds);
        }
        docOrds[upto++] = ord;
      }
      ords[newDocID] = ArrayUtil.copyOfSubArray(docOrds, 0, upto);
    }
    return ords;
  }

  @Override
  Sorter.DocComparator getDocComparator(int maxDoc, SortField sortField) throws IOException {
    assert sortField instanceof SortedSetSortField;
    assert finalOrds == null && finalOrdCounts == null && finalSortedValues == null && finalOrdMap == null;
    int valueCount = hash.size();
    finalOrds = pending.build();
    finalOrdCounts = pendingCounts.build();
    finalSortedValues = hash.sort();
    finalOrdMap = new int[valueCount];
    for (int ord = 0; ord < valueCount; ord++) {
      finalOrdMap[finalSortedValues[ord]] = ord;
    }

    SortedSetSortField sf = (SortedSetSortField) sortField;
    final SortedSetDocValues dvs =
        new BufferedSortedSetDocValues(finalSortedValues, finalOrdMap, hash, finalOrds, finalOrdCounts, maxCount, docsWithField.iterator());
    return Sorter.getDocComparator(maxDoc, sf, () -> SortedSetSelector.wrap(dvs, sf.getSelector()), () -> null);
  }

  @Override
  public void flush(SegmentWriteState state, Sorter.DocMap sortMap, DocValuesConsumer dvConsumer) throws IOException {
    final int valueCount = hash.size();
    final PackedLongValues ords;
    final PackedLongValues ordCounts;
    final int[] sortedValues;
    final int[] ordMap;

        if (finalOrdCounts == null) {
            ords = pending.build();
            ordCounts = pendingCounts.build();
            // 这个方法是对ids[]数组进行排序，因为下标是hash值，所以termID会分散在数组的不同位置
            // 调用这个方法后，所有termID的值都处在数组最前面, 并且按照termID对应的域值排序(重要)
            sortedValues = hash.sort();
            //trick: 调用结束后，ordMap数组的下标是termID，数组元素用来表示这个termID在sortedValues数组中的下标值
            ordMap = new int[valueCount];
            for(int ord=0;ord<valueCount;ord++) {
                ordMap[sortedValues[ord]] = ord;
            }
        } else {
            ords = finalOrds;
            ordCounts = finalOrdCounts;
            sortedValues = finalSortedValues;
            ordMap = finalOrdMap;
        }

    final long[][] sorted;
    if (sortMap != null) {
      sorted = sortDocValues(state.segmentInfo.maxDoc(), sortMap,
          new BufferedSortedSetDocValues(sortedValues, ordMap, hash, ords, ordCounts, maxCount, docsWithField.iterator()));
    } else {
      sorted = null;
    }
    dvConsumer.addSortedSetField(fieldInfo,
                                 new EmptyDocValuesProducer() {
                                   @Override
                                   public SortedSetDocValues getSortedSet(FieldInfo fieldInfoIn) {
                                     if (fieldInfoIn != fieldInfo) {
                                       throw new IllegalArgumentException("wrong fieldInfo");
                                     }
                                     final SortedSetDocValues buf =
                                         new BufferedSortedSetDocValues(sortedValues, ordMap, hash, ords, ordCounts, maxCount, docsWithField.iterator());
                                     if (sorted == null) {
                                       return buf;
                                     } else {
                                       return new SortingLeafReader.SortingSortedSetDocValues(buf, sorted);
                                     }
                                   }
                                 });
  }

  private static class BufferedSortedSetDocValues extends SortedSetDocValues {
    final int[] sortedValues;
    final int[] ordMap;
    final BytesRefHash hash;
    final BytesRef scratch = new BytesRef();
    final PackedLongValues.Iterator ordsIter;
    final PackedLongValues.Iterator ordCountsIter;
    final DocIdSetIterator docsWithField;
    final int currentDoc[];
    
    private int ordCount;
    // 用来描述正在使用currentDoc[]数组的位置
    private int ordUpto;

    public BufferedSortedSetDocValues(int[] sortedValues, int[] ordMap, BytesRefHash hash, PackedLongValues ords, PackedLongValues ordCounts, int maxCount, DocIdSetIterator docsWithField) {
      this.currentDoc = new int[maxCount];
      this.sortedValues = sortedValues;
      this.ordMap = ordMap;
      this.hash = hash;
      this.ordsIter = ords.iterator();
      this.ordCountsIter = ordCounts.iterator();
      this.docsWithField = docsWithField;
    }

    @Override
    public int docID() {
      return docsWithField.docID();
    }

    @Override
    public int nextDoc() throws IOException {
        // 获取文档号
      int docID = docsWithField.nextDoc();
      if (docID != NO_MORE_DOCS) {
          // 获取当前文档中包含的域值个数(去重)
        ordCount = (int) ordCountsIter.next();
        assert ordCount > 0;
        // ordsIter.next()方法用来一个一个的取出当前文档的所有termID(termID是从小到大排序的)
        // ordMap[]根据下标(termID)取出一个数组元素，这个数组元素用来作为sortedValues[]数组的下标值
        for (int i = 0; i < ordCount; i++) {
          currentDoc[i] = ordMap[Math.toIntExact(ordsIter.next())];
        }
        Arrays.sort(currentDoc, 0, ordCount);
        ordUpto = 0;
      }
      return docID;
    }

    @Override
    public long nextOrd() {
      if (ordUpto == ordCount) {
        return NO_MORE_ORDS;
      } else {
         // 返回 sortedValues[]数组的一个下标值
        return currentDoc[ordUpto++];
      }
    }

    @Override
    public long cost() {
      return docsWithField.cost();
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
    public long getValueCount() {
      return ordMap.length;
    }

    @Override
    public BytesRef lookupOrd(long ord) {
      assert ord >= 0 && ord < ordMap.length: "ord=" + ord + " is out of bounds 0 .. " + (ordMap.length-1);
      hash.get(sortedValues[Math.toIntExact(ord)], scratch);
      return scratch;
    }
  }
  @Override
  DocIdSetIterator getDocIdSet() {
    return docsWithField.iterator();
  }
}
