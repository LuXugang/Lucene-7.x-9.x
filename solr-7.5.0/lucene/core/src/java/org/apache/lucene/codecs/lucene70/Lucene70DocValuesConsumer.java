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
package org.apache.lucene.codecs.lucene70;


import static org.apache.lucene.codecs.lucene70.Lucene70DocValuesFormat.DIRECT_MONOTONIC_BLOCK_SHIFT;
import static org.apache.lucene.codecs.lucene70.Lucene70DocValuesFormat.NUMERIC_BLOCK_SHIFT;
import static org.apache.lucene.codecs.lucene70.Lucene70DocValuesFormat.NUMERIC_BLOCK_SIZE;

import java.io.Closeable; // javadocs
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.EmptyDocValuesProducer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.SortedSetSelector;
import org.apache.lucene.store.GrowableByteArrayDataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.MathUtil;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.packed.DirectMonotonicWriter;
import org.apache.lucene.util.packed.DirectWriter;

/** writer for {@link Lucene70DocValuesFormat} */
final class Lucene70DocValuesConsumer extends DocValuesConsumer implements Closeable {

  IndexOutput data, meta;
  final int maxDoc;

  /** expert: Creates a new writer */
  // 除了第一个参数，其他参数都是固定值
  public Lucene70DocValuesConsumer(SegmentWriteState state, String dataCodec, String dataExtension, String metaCodec, String metaExtension) throws IOException {
    boolean success = false;
    try {
      // 设置data的名字，data即存放DocValue的对象, dvd是DocValue Data的简写
      // 例子：dataName的名字可能是 _0_Lucene70_0.dvd, 这个名字由三部分组成"_0", "Lucene70_0", "dvd"
      String dataName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, dataExtension);
      data = state.directory.createOutput(dataName, state.context);
      // 写入dvd文件索引头的数据 包括的数据：
      // 1. 描述索引首位置的一个固定值：0x3fd76c17
      // 2. 描述索引内容的String值: "Lucene70DocValuesData"
      // 3. 版本号: 固定值, 参考Lucene70DocValuesFormat.VERSION_CURRENT的值
      // 4. 唯一标识索引文件的值
      // 5. 索引文件的后缀名字长度：用来描述在读取索引文件时候该读取多少个字节(数据是连续存储的)
      // 6. 索引文件的后缀名字 String类型: Lucene70_0
      CodecUtil.writeIndexHeader(data, dataCodec, Lucene70DocValuesFormat.VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
      // 设置mete的名字，mete即存放的数据用来映射data中的数据, dvm 是DocValue meta的简写
      // 例子：metaName的名字可能是 _0_Lucene70_0.dvm, 这个名字由三部分组成"_0", "Lucene70_0", "dvd"
      String metaName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, metaExtension);
      meta = state.directory.createOutput(metaName, state.context);
      // 写入dvm文件索引头的数据 包括的数据跟dvd类似, 不重复：
      CodecUtil.writeIndexHeader(meta, metaCodec, Lucene70DocValuesFormat.VERSION_CURRENT, state.segmentInfo.getId(), state.segmentSuffix);
      maxDoc = state.segmentInfo.maxDoc();
      success = true;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(this);
      }
    }
  }

  @Override
  public void close() throws IOException {
    boolean success = false;
    try {
      if (meta != null) {
        meta.writeInt(-1); // write EOF marker
        CodecUtil.writeFooter(meta); // write checksum
      }
      if (data != null) {
        CodecUtil.writeFooter(data); // write checksum
      }
      success = true;
    } finally {
      if (success) {
        IOUtils.close(data, meta);
      } else {
        IOUtils.closeWhileHandlingException(data, meta);
      }
      meta = data = null;
    }
  }

  @Override
  public void addNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    meta.writeInt(field.number);
    meta.writeByte(Lucene70DocValuesFormat.NUMERIC);

    writeValues(field, new EmptyDocValuesProducer() {
      @Override
      public SortedNumericDocValues getSortedNumeric(FieldInfo field) throws IOException {
        return DocValues.singleton(valuesProducer.getNumeric(field));
      }
    });
  }

  private static class MinMaxTracker {
    long min, max, numValues, spaceInBits;

    MinMaxTracker() {
      reset();
      spaceInBits = 0;
    }

    private void reset() {
      min = Long.MAX_VALUE;
      max = Long.MIN_VALUE;
      numValues = 0;
    }

    /** Accumulate a new value. */
    void update(long v) {
      min = Math.min(min, v);
      max = Math.max(max, v);
      ++numValues;
    }

    /** Update the required space. */
    void finish() {
      if (max > min) {
        spaceInBits += DirectWriter.unsignedBitsRequired(max - min) * numValues;
      }
    }

    /** Update space usage and get ready for accumulating values for the next block. */
    void nextBlock() {
      finish();
      reset();
    }
  }

  private long[] writeValues(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    SortedNumericDocValues values = valuesProducer.getSortedNumeric(field);
    int numDocsWithValue = 0;
    MinMaxTracker minMax = new MinMaxTracker();
    MinMaxTracker blockMinMax = new MinMaxTracker();
    // gcd： greatest common divisor 最大公约数
    // 计算gcd的目的在于，能在存储时候减少空间，比如说存储9000，8000，7000, 6000他们的最大公约数为1000
    // 那么实际存储每个值需要的bit为为 （9000 -6000) / 1000 = 3。只要一个bit位就能描述一个域值
    long gcd = 0;
    // uniqueValues来记录域值的种类（去重）。
    Set<Long> uniqueValues = new HashSet<>();
    // 遍历的次数为包含当前域的文档个数
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      for (int i = 0, count = values.docValueCount(); i < count; ++i) {
        long v = values.nextValue();

        if (gcd != 1) {
          if (v < Long.MIN_VALUE / 2 || v > Long.MAX_VALUE / 2) {
            // in that case v - minValue might overflow and make the GCD computation return
            // wrong results. Since these extreme values are unlikely, we just discard
            // GCD computation for them
            gcd = 1;
          } else if (minMax.numValues != 0) { // minValue needs to be set first
            gcd = MathUtil.gcd(gcd, v - minMax.min);
          }
        }

        // 更新最大值跟最小值
        minMax.update(v);
        // 更新最大值跟最小值, 当域值的个数大于NUMERIC_BLOCK_SIZE时，blockMinMax用来判断如果按块处理是否能降低存储空间
        // 为何能降低存储空间请看博客介绍
        blockMinMax.update(v);
        if (blockMinMax.numValues == NUMERIC_BLOCK_SIZE) {
          blockMinMax.nextBlock();
        }
        //  统计不相同的域值
        if (uniqueValues != null
            && uniqueValues.add(v)
            && uniqueValues.size() > 256) {
          uniqueValues = null;
        }
      }

      // numDocsWithValue统计域值的总数（非去重）
      numDocsWithValue++;
    }

    minMax.finish();
    blockMinMax.finish();

    final long numValues = minMax.numValues;
    // 当前域中最小的值
    long min = minMax.min;
    // 当前域中最大的值
    final long max = minMax.max;
    assert blockMinMax.spaceInBits <= minMax.spaceInBits;
    // start: 记录包含当前域的文档号
    if (numDocsWithValue == 0) {
      meta.writeLong(-2);
      meta.writeLong(0L);
      // 当前IndexWrite添加的每一个document中都包含当前域，那么就不用记录文档号了
    } else if (numDocsWithValue == maxDoc) {
      meta.writeLong(-1);
      meta.writeLong(0L);
    } else {
      // 当前IndexWrite添加的document中并不是所有的document都包含当前域，需要存储具体的文档号
      long offset = data.getFilePointer();
      meta.writeLong(offset);
      values = valuesProducer.getSortedNumeric(field);
      IndexedDISI.writeBitSet(values, data);
      meta.writeLong(data.getFilePointer() - offset);
    }
    // end: 记录包含当前域的文档号

    meta.writeLong(numValues);
    final int numBitsPerValue;
    boolean doBlocks = false;
    Map<Long, Integer> encode = null;
    if (min >= max) {
      numBitsPerValue = 0;
      meta.writeInt(-1);
    } else {
      if (uniqueValues != null
          && uniqueValues.size() > 1
          && DirectWriter.unsignedBitsRequired(uniqueValues.size() - 1) < DirectWriter.unsignedBitsRequired((max - min) / gcd)) {
        // 存储不相同的域值的个数值需要用多少bit位, 注意的是这里使用的是DirectWriter中的unsignedBitsRequired方法
        // 跟PackedInts.unsignedBitsRequired不同的是，实际返回的bit的个数是DirectWriter中的SUPPORTED_BITS_PER_VALUE数组中的值
        numBitsPerValue = DirectWriter.unsignedBitsRequired(uniqueValues.size() - 1);
        // sortedUniqueValues数组中存放所有的去重的域值，然后排序
        final Long[] sortedUniqueValues = uniqueValues.toArray(new Long[0]);
        Arrays.sort(sortedUniqueValues);
        meta.writeInt(sortedUniqueValues.length);
        // 写入所有的去重的域值
        // 这里在dvm文件中写入域值的目的是，在dvd文件就不用写真实的域值，而是写入encode的value值
        // 在读取阶段，就可以通过value从dvm中获得真实的域值
        // 这么做的好处在于，能优化域值中的最大值跟最小值跨度很大，导致数值较小的域值也要分配多余的bit位
        for (Long v : sortedUniqueValues) {
          meta.writeLong(v);
        }
        // HashMap的key为域值，value描述了这个域值在所有去重的域值中的大小关系
        encode = new HashMap<>();
        for (int i = 0; i < sortedUniqueValues.length; ++i) {
          encode.put(sortedUniqueValues[i], i);
        }
        min = 0;
        gcd = 1;
      } else {
        uniqueValues = null;
        // we do blocks if that appears to save 10+% storage
        // spaceInBits指的的是存储所有的值需要的bit位总数
        doBlocks = minMax.spaceInBits > 0 && (double) blockMinMax.spaceInBits / minMax.spaceInBits <= 0.9;
        // doBlocks如果为true，说明按块处理域值能至少降低10%存储空间, 可能更多
        if (doBlocks) {
          numBitsPerValue = 0xFF;
          meta.writeInt(-2 - NUMERIC_BLOCK_SHIFT);
        } else {
          numBitsPerValue = DirectWriter.unsignedBitsRequired((max - min) / gcd);
          if (gcd == 1 && min > 0
              && DirectWriter.unsignedBitsRequired(max) == DirectWriter.unsignedBitsRequired(max - min)) {
            min = 0;
          }
          meta.writeInt(-1);
        }
      }
    }

    meta.writeByte((byte) numBitsPerValue);
    // 记录最小值，在读取阶段需要利用min来恢复数据
    meta.writeLong(min);
    // 记录最大公约数，在读取阶段需要利用gcd来恢复数据
    meta.writeLong(gcd);
    long startOffset = data.getFilePointer();
    meta.writeLong(startOffset);
    if (doBlocks) {
      writeValuesMultipleBlocks(valuesProducer.getSortedNumeric(field), gcd);
    } else if (numBitsPerValue != 0) {
      writeValuesSingleBlock(valuesProducer.getSortedNumeric(field), numValues, numBitsPerValue, min, gcd, encode);
    }
    meta.writeLong(data.getFilePointer() - startOffset);

    return new long[] {numDocsWithValue, numValues};
  }

  private void writeValuesSingleBlock(SortedNumericDocValues values, long numValues, int numBitsPerValue,
      long min, long gcd, Map<Long, Integer> encode) throws IOException {
    DirectWriter writer = DirectWriter.getInstance(data, numValues, numBitsPerValue);
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      for (int i = 0, count = values.docValueCount(); i < count; ++i) {
        long v = values.nextValue();
        if (encode == null) {
          writer.add((v - min) / gcd);
        } else {
          writer.add(encode.get(v));
        }
      }
    }
    writer.finish();
  }
 
  private void writeValuesMultipleBlocks(SortedNumericDocValues values, long gcd) throws IOException {
    final long[] buffer = new long[NUMERIC_BLOCK_SIZE];
    final GrowableByteArrayDataOutput encodeBuffer = new GrowableByteArrayDataOutput(NUMERIC_BLOCK_SIZE);
    int upTo = 0;
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      for (int i = 0, count = values.docValueCount(); i < count; ++i) {
        // 将每一给域值写入到buffer数组中，当个数达到NUMERIC_BLOCK_SIZE时候，再进行压缩存储,压缩后的数据写入到encodeBuffer数组中
        buffer[upTo++] = values.nextValue();
        if (upTo == NUMERIC_BLOCK_SIZE) {
          writeBlock(buffer, NUMERIC_BLOCK_SIZE, gcd, encodeBuffer);
          upTo = 0;
        }
      }
    }
    // 如果upTo大于0，说明剩余的域值个数小于NUMERIC_BLOCK_SIZE，并且这些域值写入到.dvd文件中
    if (upTo > 0) {
      writeBlock(buffer, upTo, gcd, encodeBuffer);
    }
  }

  private void writeBlock(long[] values, int length, long gcd, GrowableByteArrayDataOutput buffer) throws IOException {
    assert length > 0;
    long min = values[0];
    long max = values[0];
    // 遍历values数组，获取数组中最大跟最小值
    for (int i = 1; i < length; ++i) {
      final long v = values[i];
      assert Math.floorMod(values[i] - min, gcd) == 0;
      min = Math.min(min, v);
      max = Math.max(max, v);
    }
    if (min == max) {
      data.writeByte((byte) 0);
      data.writeLong(min);
    } else {
      // 根据上面得到的最大跟最小值，算出存储每一个值需要的bit位
      final int bitsPerValue = DirectWriter.unsignedBitsRequired(max - min);
      buffer.reset();
      assert buffer.getPosition() == 0;
      final DirectWriter w = DirectWriter.getInstance(buffer, length, bitsPerValue);
      // 将每一个域值使用DirectWriter处理并压缩
      for (int i = 0; i < length; ++i) {
        w.add((values[i] - min) / gcd);
      }
      w.finish();
      data.writeByte((byte) bitsPerValue);
      // 记录最小值是为了读取阶段能恢复压缩的数据
      data.writeLong(min);
      // 记录数据长度
      data.writeInt(buffer.getPosition());
      // 根据数据长度写入数据
      data.writeBytes(buffer.getBytes(), buffer.getPosition());
    }
  }

  @Override
  public void addBinaryField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    meta.writeInt(field.number);
    meta.writeByte(Lucene70DocValuesFormat.BINARY);

    BinaryDocValues values = valuesProducer.getBinary(field);
    long start = data.getFilePointer();
    meta.writeLong(start);
    int numDocsWithField = 0;
    int minLength = Integer.MAX_VALUE;
    int maxLength = 0;
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      numDocsWithField++;
      BytesRef v = values.binaryValue();
      int length = v.length;
      data.writeBytes(v.bytes, v.offset, v.length);
      minLength = Math.min(length, minLength);
      maxLength = Math.max(length, maxLength);
    }
    assert numDocsWithField <= maxDoc;
    meta.writeLong(data.getFilePointer() - start);

    if (numDocsWithField == 0) {
      meta.writeLong(-2);
      meta.writeLong(0L);
    } else if (numDocsWithField == maxDoc) {
      meta.writeLong(-1);
      meta.writeLong(0L);
    } else {
      long offset = data.getFilePointer();
      meta.writeLong(offset);
      values = valuesProducer.getBinary(field);
      IndexedDISI.writeBitSet(values, data);
      meta.writeLong(data.getFilePointer() - offset);
    }

    meta.writeInt(numDocsWithField);
    meta.writeInt(minLength);
    meta.writeInt(maxLength);
    if (maxLength > minLength) {
      start = data.getFilePointer();
      meta.writeLong(start);
      meta.writeVInt(DIRECT_MONOTONIC_BLOCK_SHIFT);

      final DirectMonotonicWriter writer = DirectMonotonicWriter.getInstance(meta, data, numDocsWithField + 1, DIRECT_MONOTONIC_BLOCK_SHIFT);
      long addr = 0;
      writer.add(addr);
      values = valuesProducer.getBinary(field);
      for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
        addr += values.binaryValue().length;
        writer.add(addr);
      }
      writer.finish();
      meta.writeLong(data.getFilePointer() - start);
    }
  }

  @Override
  public void addSortedField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    meta.writeInt(field.number);
    meta.writeByte(Lucene70DocValuesFormat.SORTED);
    // valuesProducer对象即SortedDocValuesWriter类的对象
    doAddSortedField(field, valuesProducer);
  }

  private void doAddSortedField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    // 根据SortedDocValuesWriter获得BufferedSortedDocValues对象
    SortedDocValues values = valuesProducer.getSorted(field);
    int numDocsWithField = 0;
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      // 统计文档个数
      numDocsWithField++;
    }

    if (numDocsWithField == 0) {
      meta.writeLong(-2);
      meta.writeLong(0L);
      // if语句为真说明 所有文档都包含当前域名的SortedDocValuesField
    } else if (numDocsWithField == maxDoc) {
      meta.writeLong(-1);
      meta.writeLong(0L);
      // 不是所有文档都包含当前域名的SortedDocValuesField
    } else {
      // 获取当前dvd文件中的偏移位置，即data对象中count的值
      long offset = data.getFilePointer();
      meta.writeLong(offset);
      values = valuesProducer.getSorted(field);
      IndexedDISI.writeBitSet(values, data);
      meta.writeLong(data.getFilePointer() - offset);
    }

    meta.writeInt(numDocsWithField);
    // 域值的种类只有一个
    if (values.getValueCount() <= 1) {
      meta.writeByte((byte) 0);
      meta.writeLong(0L);
      meta.writeLong(0L);
      // 有多个域值
    } else {
      // 获得存储域值种类需要的bit位，例如 3种域值，只要2个bit位即可
      int numberOfBitsPerOrd = DirectWriter.unsignedBitsRequired(values.getValueCount() - 1);
      meta.writeByte((byte) numberOfBitsPerOrd);
      long start = data.getFilePointer();
      meta.writeLong(start);
      DirectWriter writer = DirectWriter.getInstance(data, numDocsWithField, numberOfBitsPerOrd);
      values = valuesProducer.getSorted(field);
      // 遍历所有文档的域值
      for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
        // 这里的values.ordValues()的值的取值范围是 0~values.getValueCount()
        // 这些值是sortedValues[]下标值(SortedDocValuesWriter类中定义)
        writer.add(values.ordValue());
      }
      writer.finish();
      meta.writeLong(data.getFilePointer() - start);
    }

    addTermsDict(DocValues.singleton(valuesProducer.getSorted(field)));
  }

  private void addTermsDict(SortedSetDocValues values) throws IOException {
    // 获取term的个数(去重)
    final long size = values.getValueCount();
    // 可变长度的存储
    meta.writeVLong(size);
    meta.writeInt(Lucene70DocValuesFormat.TERMS_DICT_BLOCK_SHIFT);

    RAMOutputStream addressBuffer = new RAMOutputStream();
    meta.writeInt(DIRECT_MONOTONIC_BLOCK_SHIFT);
    long numBlocks = (size + Lucene70DocValuesFormat.TERMS_DICT_BLOCK_MASK) >>> Lucene70DocValuesFormat.TERMS_DICT_BLOCK_SHIFT;
    DirectMonotonicWriter writer = DirectMonotonicWriter.getInstance(meta, addressBuffer, numBlocks, DIRECT_MONOTONIC_BLOCK_SHIFT);

    BytesRefBuilder previous = new BytesRefBuilder();
    long ord = 0;
    // 获取data中的buf[]数组可以使用的一个位置
    long start = data.getFilePointer();
    int maxLength = 0;
    // 定义一个SortedDocValuesTermsEnum对象
    TermsEnum iterator = values.termsEnum();
    // 每次获得term的时间复杂度是O(1)
    for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
      // if条件满足，说明是一个block中的第一个term
      if ((ord & Lucene70DocValuesFormat.TERMS_DICT_BLOCK_MASK) == 0) {
        // 记录每一个term相对于start位置占用的byte个数, 查询的时候会用到这个值用来定位一个term在data中的起始位置
        writer.add(data.getFilePointer() - start);
        // 记录term的长度，在读取时候用来确定读取的范围
        data.writeVInt(term.length);
        // 将完整的term值写入到dvd文件中
        data.writeBytes(term.bytes, term.offset, term.length);
      } else {
        // 计算当前处理的term跟前一个term(previous)(ByteRef对象)相同前缀的长度
        final int prefixLength = StringHelper.bytesDifference(previous.get(), term);
        // 计算跟前一个term不一样的部分的长度
        final int suffixLength = term.length - prefixLength;
        assert suffixLength > 0; // terms are unique
        // 一个字节的低4位存放prefixLength的值，高4位存放suffixLength的值
        data.writeByte((byte) (Math.min(prefixLength, 15) | (Math.min(15, suffixLength - 1) << 4)));
        // 上行代码中只能存放prefixLength最大值为15和suffixLength最大值为16，那么如果两者超出最大值，需要把多出的部分写入到dvd文件中
        if (prefixLength >= 15) {
          data.writeVInt(prefixLength - 15);
        }
        // 将后缀超过16个byte的部分再次写入到data中
        if (suffixLength >= 16) {
          data.writeVInt(suffixLength - 16);
        }
        // 只将term的后缀部分写入到data中(term的前缀没有存储)
        data.writeBytes(term.bytes, term.offset + prefixLength, term.length - prefixLength);
      }
      // 记录所有term中长度最大的值
      maxLength = Math.max(maxLength, term.length);
      // 处理下一个term时候，先保存当前的term值, 用来做前缀处理
      previous.copyBytes(term);
      ++ord;
    }
    // writer记录的东西没有明白用来干嘛，斜率 最小值。。。啥意思啊, 不知道干嘛用
    writer.finish();
    // 记录所有term长度最大的值
    meta.writeInt(maxLength);
    // 记录data的buf[]数组的起始位置, 用来映射上面记录的数据
    meta.writeLong(start);
    // 记录data的buf[]数组的结束位置, 用来映射上面记录的数据
    meta.writeLong(data.getFilePointer() - start);

    start = data.getFilePointer();
    addressBuffer.writeTo(data);
    // 不写你也应该猜出这个是干嘛的, addressBuffer.writeTo(data)的操作又在data中新增了数据呗
    meta.writeLong(start);
    // 不写你也应该猜出这个是干嘛的
    meta.writeLong(data.getFilePointer() - start);

    // Now write the reverse terms index
    writeTermsIndex(values);
  }

  private void writeTermsIndex(SortedSetDocValues values) throws IOException {
    // 获取term的个数(去重)
    final long size = values.getValueCount();
    meta.writeInt(Lucene70DocValuesFormat.TERMS_DICT_REVERSE_INDEX_SHIFT);
    long start = data.getFilePointer();

    long numBlocks = 1L + ((size + Lucene70DocValuesFormat.TERMS_DICT_REVERSE_INDEX_MASK) >>> Lucene70DocValuesFormat.TERMS_DICT_REVERSE_INDEX_SHIFT);
    RAMOutputStream addressBuffer = new RAMOutputStream();
    DirectMonotonicWriter writer = DirectMonotonicWriter.getInstance(meta, addressBuffer, numBlocks, DIRECT_MONOTONIC_BLOCK_SHIFT);

    TermsEnum iterator = values.termsEnum();
    BytesRefBuilder previous = new BytesRefBuilder();
    long offset = 0;
    long ord = 0;
    // 按照term值有序的取出
    for (BytesRef term = iterator.next(); term != null; term = iterator.next()) {
      if ((ord & Lucene70DocValuesFormat.TERMS_DICT_REVERSE_INDEX_MASK) == 0) {
        writer.add(offset);
        final int sortKeyLength;
        if (ord == 0) {
          // no previous term: no bytes to write
          sortKeyLength = 0;
        } else {
          // 计算出两个term相同前缀的长度
          sortKeyLength = StringHelper.sortKeyLength(previous.get(), term);
        }
        offset += sortKeyLength;
        data.writeBytes(term.bytes, term.offset, sortKeyLength);
      } else if ((ord & Lucene70DocValuesFormat.TERMS_DICT_REVERSE_INDEX_MASK) == Lucene70DocValuesFormat.TERMS_DICT_REVERSE_INDEX_MASK) {
        previous.copyBytes(term);
      }
      ++ord;
    }
    writer.add(offset);
    writer.finish();
    meta.writeLong(start);
    meta.writeLong(data.getFilePointer() - start);
    start = data.getFilePointer();
    addressBuffer.writeTo(data);
    meta.writeLong(start);
    meta.writeLong(data.getFilePointer() - start);
  }

  @Override
  public void addSortedNumericField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    meta.writeInt(field.number);
    meta.writeByte(Lucene70DocValuesFormat.SORTED_NUMERIC);

    long[] stats = writeValues(field, valuesProducer);
    int numDocsWithField = Math.toIntExact(stats[0]);
    long numValues = stats[1];
    assert numValues >= numDocsWithField;

    meta.writeInt(numDocsWithField);
    if (numValues > numDocsWithField) {
      long start = data.getFilePointer();
      meta.writeLong(start);
      meta.writeVInt(DIRECT_MONOTONIC_BLOCK_SHIFT);

      final DirectMonotonicWriter addressesWriter = DirectMonotonicWriter.getInstance(meta, data, numDocsWithField + 1L, DIRECT_MONOTONIC_BLOCK_SHIFT);
      long addr = 0;
      addressesWriter.add(addr);
      SortedNumericDocValues values = valuesProducer.getSortedNumeric(field);
      for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
        addr += values.docValueCount();
        addressesWriter.add(addr);
      }
      addressesWriter.finish();
      meta.writeLong(data.getFilePointer() - start);
    }
  }

  @Override
  public void addSortedSetField(FieldInfo field, DocValuesProducer valuesProducer) throws IOException {
    meta.writeInt(field.number);
    meta.writeByte(Lucene70DocValuesFormat.SORTED_SET);

    // SortedSetDocValuesWriter中的BufferedSortedSetDocValues内部类对象
    SortedSetDocValues values = valuesProducer.getSortedSet(field);
    int numDocsWithField = 0;
    long numOrds = 0;
    // doc为文档号
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      numDocsWithField++;
      for (long ord = values.nextOrd(); ord != SortedSetDocValues.NO_MORE_ORDS; ord = values.nextOrd()) {
        // 统计当前文档号的termID的个数(去重)
        numOrds++;
      }
    }
    // 判断文档个数是否跟所有文档包含的termID总数 一样多
    if (numDocsWithField == numOrds) {
      meta.writeByte((byte) 0);
      doAddSortedField(field, new EmptyDocValuesProducer() {
        @Override
        public SortedDocValues getSorted(FieldInfo field) throws IOException {
          return SortedSetSelector.wrap(valuesProducer.getSortedSet(field), SortedSetSelector.Type.MIN);
        }
      });
      return;
    }
    meta.writeByte((byte) 1);

    assert numDocsWithField != 0;
    if (numDocsWithField == maxDoc) {
      // 写8个字节
      meta.writeLong(-1);
      meta.writeLong(0L);
    } else {
      long offset = data.getFilePointer();
      meta.writeLong(offset);
      values = valuesProducer.getSortedSet(field);
      IndexedDISI.writeBitSet(values, data);
      meta.writeLong(data.getFilePointer() - offset);
    }

    //values.getValueCount()取出是ordMap[]数组中元素的个数
    // numberOfBitsPerOrd用来记录存储ordMap[]的元素的个数需要的bit位
    int numberOfBitsPerOrd = DirectWriter.unsignedBitsRequired(values.getValueCount() - 1);
    meta.writeByte((byte) numberOfBitsPerOrd);
    // 获取dvd文件中当前可以写入数据的位置
    long start = data.getFilePointer();
    // 在meta中记录这个位置. 作为映射使用
    meta.writeLong(start);
    // numOrds是每个文档包含的term的个数总和
    DirectWriter writer = DirectWriter.getInstance(data, numOrds, numberOfBitsPerOrd);
    // 这里重新获得一次SortedSetDocValuesWriter类中的BufferedSortedSetDocValues对象,因为没有reset()方法来重置一些状态的吗? 哈哈
    values = valuesProducer.getSortedSet(field);
    // 遍历所有的document
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      // 遍历某一个document的所有termID
      for (long ord = values.nextOrd(); ord != SortedSetDocValues.NO_MORE_ORDS; ord = values.nextOrd()) {
        // 依次写入每篇文档的包含的termID
        writer.add(ord);
      }
    }
    // 将数据(encode后的数据)写入到data对象中
    writer.finish();
    // 将 记录所有文档ord值所需要byte位个数(data对象的buf中)记录到meta中
    meta.writeLong(data.getFilePointer() - start);

    // 记录文档个数
    meta.writeInt(numDocsWithField);
    start = data.getFilePointer();
    //在meta中记录data的一个位置, 一个开始的位置 作为映射使用
    meta.writeLong(start);
    // 使用VInt类型来记录int类型，使得最少能用一个byte记录一个int值
    meta.writeVInt(DIRECT_MONOTONIC_BLOCK_SHIFT);

    final DirectMonotonicWriter addressesWriter = DirectMonotonicWriter.getInstance(meta, data, numDocsWithField + 1, DIRECT_MONOTONIC_BLOCK_SHIFT);
    long addr = 0;
    addressesWriter.add(addr);
    values = valuesProducer.getSortedSet(field);
    for (int doc = values.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = values.nextDoc()) {
      values.nextOrd();
      addr++;
      while (values.nextOrd() != SortedSetDocValues.NO_MORE_ORDS) {
        addr++;
      }
      addressesWriter.add(addr);
    }
    // addressWriter记录的东西没有明白用来干嘛，斜率 最小值。。。啥意思啊
    addressesWriter.finish();
    // 记录 data中的一个位置，一个结束位置，也是用来映射
    meta.writeLong(data.getFilePointer() - start);

    addTermsDict(values);
  }
}
