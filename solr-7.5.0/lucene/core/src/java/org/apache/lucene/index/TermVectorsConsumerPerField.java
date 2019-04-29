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

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.codecs.TermVectorsWriter;
import org.apache.lucene.util.BytesRef;

final class TermVectorsConsumerPerField extends TermsHashPerField {

  private TermVectorsPostingsArray termVectorsPostingsArray;

  final TermVectorsConsumer termsWriter;

  boolean doVectors;
  boolean doVectorPositions;
  boolean doVectorOffsets;
  boolean doVectorPayloads;

  OffsetAttribute offsetAttribute;
  PayloadAttribute payloadAttribute;
  boolean hasPayloads; // if enabled, and we actually saw any for this field

  public TermVectorsConsumerPerField(FieldInvertState invertState, TermVectorsConsumer termsWriter, FieldInfo fieldInfo) {
    super(2, invertState, termsWriter, null, fieldInfo);
    this.termsWriter = termsWriter;
  }

  /** Called once per field per document if term vectors
   *  are enabled, to write the vectors to
   *  RAMOutputStream, which is then quickly flushed to
   *  the real term vectors files in the Directory. */  @Override
  void finish() {
    if (!doVectors || bytesHash.size() == 0) {
      return;
    }
    termsWriter.addFieldToFlush(this);
  }

  void finishDocument() throws IOException {
    if (doVectors == false) {
      return;
    }

    doVectors = false;

    // 当前域中的term种类
    final int numPostings = bytesHash.size();

    final BytesRef flushTerm = termsWriter.flushTerm;

    assert numPostings >= 0;

    // This is called once, after inverting all occurrences
    // of a given field in the doc.  At this point we flush
    // our hash into the DocWriter.

    // 获取倒排表数据, 即ParallelPostingsArray对象中各种数组、例如 freq[]、lastOffset[]数组等等
    TermVectorsPostingsArray postings = termVectorsPostingsArray;
    final TermVectorsWriter tv = termsWriter.writer;

    // termIDs数组中的元素是有序的，不过排序规则不是根据数组元素的大小，而是数组元素对应的term值
    // 排序的目的是为了能前缀存储
    final int[] termIDs = sortPostings();

    tv.startField(fieldInfo, numPostings, doVectorPositions, doVectorOffsets, hasPayloads);

    // 初始化一个读取term 位置&&payload的对象
    final ByteSliceReader posReader = doVectorPositions ? termsWriter.vectorSliceReaderPos : null;
    // 初始化一个读取term offset信息的对象
    final ByteSliceReader offReader = doVectorOffsets ? termsWriter.vectorSliceReaderOff : null;

    for(int j=0;j<numPostings;j++) {
      final int termID = termIDs[j];
      // term在本篇文档中的词频
      final int freq = postings.freqs[termID];

      // Get BytesRef
      // 获取当前term的信息，由于TermVector词向量生成的倒排表没有term信息，所以需要从 STORE.YES生成的倒排表中获取
      termBytePool.setBytesRef(flushTerm, postings.textStarts[termID]);
      tv.startTerm(flushTerm, freq);
      
      if (doVectorPositions || doVectorOffsets) {
        if (posReader != null) {
          initReader(posReader, termID, 0);
        }
        if (offReader != null) {
          initReader(offReader, termID, 1);
        }
        tv.addProx(freq, posReader, offReader);
      }
      tv.finishTerm();
    }
    tv.finishField();

    // 清楚当前域的所有倒排表数据
    reset();

    fieldInfo.setStoreTermVectors();
  }

  @Override
  boolean start(IndexableField field, boolean first) {
    super.start(field, first);
    assert field.fieldType().indexOptions() != IndexOptions.NONE;

    if (first) {

      if (bytesHash.size() != 0) {
        // Only necessary if previous doc hit a
        // non-aborting exception while writing vectors in
        // this field:
        reset();
      }

      bytesHash.reinit();

      hasPayloads = false;

      // 判断当前域是否存储词向量
      doVectors = field.fieldType().storeTermVectors();

      if (doVectors) {

        termsWriter.hasVectors = true;

        doVectorPositions = field.fieldType().storeTermVectorPositions();

        // Somewhat confusingly, unlike postings, you are
        // allowed to index TV offsets without TV positions:
        doVectorOffsets = field.fieldType().storeTermVectorOffsets();

        if (doVectorPositions) {
          doVectorPayloads = field.fieldType().storeTermVectorPayloads();
        } else {
          doVectorPayloads = false;
          if (field.fieldType().storeTermVectorPayloads()) {
            // TODO: move this check somewhere else, and impl the other missing ones
            throw new IllegalArgumentException("cannot index term vector payloads without term vector positions (field=\"" + field.name() + "\")");
          }
        }
        
      } else {
        if (field.fieldType().storeTermVectorOffsets()) {
          throw new IllegalArgumentException("cannot index term vector offsets when term vectors are not indexed (field=\"" + field.name() + "\")");
        }
        if (field.fieldType().storeTermVectorPositions()) {
          throw new IllegalArgumentException("cannot index term vector positions when term vectors are not indexed (field=\"" + field.name() + "\")");
        }
        if (field.fieldType().storeTermVectorPayloads()) {
          throw new IllegalArgumentException("cannot index term vector payloads when term vectors are not indexed (field=\"" + field.name() + "\")");
        }
      }
    } else {
      if (doVectors != field.fieldType().storeTermVectors()) {
        throw new IllegalArgumentException("all instances of a given field name must have the same term vectors settings (storeTermVectors changed for field=\"" + field.name() + "\")");
      }
      if (doVectorPositions != field.fieldType().storeTermVectorPositions()) {
        throw new IllegalArgumentException("all instances of a given field name must have the same term vectors settings (storeTermVectorPositions changed for field=\"" + field.name() + "\")");
      }
      if (doVectorOffsets != field.fieldType().storeTermVectorOffsets()) {
        throw new IllegalArgumentException("all instances of a given field name must have the same term vectors settings (storeTermVectorOffsets changed for field=\"" + field.name() + "\")");
      }
      if (doVectorPayloads != field.fieldType().storeTermVectorPayloads()) {
        throw new IllegalArgumentException("all instances of a given field name must have the same term vectors settings (storeTermVectorPayloads changed for field=\"" + field.name() + "\")");
      }
    }

    if (doVectors) {
      if (doVectorOffsets) {
        offsetAttribute = fieldState.offsetAttribute;
        assert offsetAttribute != null;
      }

      if (doVectorPayloads) {
        // Can be null:
        payloadAttribute = fieldState.payloadAttribute;
      } else {
        payloadAttribute = null;
      }
    }

    return doVectors;
  }
  
  void writeProx(TermVectorsPostingsArray postings, int termID) {
     //  记录offset信息
    if (doVectorOffsets) {
      // startOffset是当前term第一个字符在当前文档中的起始位置
      int startOffset = fieldState.offset + offsetAttribute.startOffset();
      //( endOffset - 1)是当前term最后一个字符在当前文档中的位置的
      int endOffset = fieldState.offset + offsetAttribute.endOffset();

      // 差值存储startOffset的信息到倒排表中
      writeVInt(1, startOffset - postings.lastOffsets[termID]);
      // term的字节长度
      writeVInt(1, endOffset - startOffset);
      // 本篇文档中再次出现当前term时用来计算差值
      postings.lastOffsets[termID] = endOffset;
    }

    // 记录position跟payload信息
    if (doVectorPositions) {
      final BytesRef payload;
      if (payloadAttribute == null) {
        payload = null;
      } else {
        payload = payloadAttribute.getPayload();
      }
      
      final int pos = fieldState.position - postings.lastPositions[termID];
      if (payload != null && payload.length > 0) {
        // 带有payload，那么pos的二进制的最低位置为1，在读取阶段表示下一个字节是payload的长度。
        writeVInt(0, (pos<<1)|1);
        // payload占用的字节长度
        writeVInt(0, payload.length);
        // payload的值
        writeBytes(0, payload.bytes, payload.offset, payload.length);
        hasPayloads = true;
      } else {
        // 没有payload，那么pos的二进制的最低位置为1，在读取阶段表示下一个字节是payload的长度。
        writeVInt(0, pos<<1);
      }
      postings.lastPositions[termID] = fieldState.position;
    }
  }

  @Override
  void newTerm(final int termID) {
    TermVectorsPostingsArray postings = termVectorsPostingsArray;

    postings.freqs[termID] = getTermFreq();
    postings.lastOffsets[termID] = 0;
    postings.lastPositions[termID] = 0;
    
    writeProx(postings, termID);
  }

  @Override
  void addTerm(final int termID) {
    TermVectorsPostingsArray postings = termVectorsPostingsArray;

    postings.freqs[termID] += getTermFreq();

    writeProx(postings, termID);
  }

  private int getTermFreq() {
    int freq = termFreqAtt.getTermFrequency();
    if (freq != 1) {
      if (doVectorPositions) {
        throw new IllegalArgumentException("field \"" + fieldInfo.name + "\": cannot index term vector positions while using custom TermFrequencyAttribute");
      }
      if (doVectorOffsets) {
        throw new IllegalArgumentException("field \"" + fieldInfo.name + "\": cannot index term vector offsets while using custom TermFrequencyAttribute");
      }
    }

    return freq;
  }

  @Override
  public void newPostingsArray() {
    termVectorsPostingsArray = (TermVectorsPostingsArray) postingsArray;
  }

  @Override
  ParallelPostingsArray createPostingsArray(int size) {
    return new TermVectorsPostingsArray(size);
  }

  static final class TermVectorsPostingsArray extends ParallelPostingsArray {
    public TermVectorsPostingsArray(int size) {
      super(size);
      freqs = new int[size];
      lastOffsets = new int[size];
      lastPositions = new int[size];
    }

    int[] freqs;                                       // How many times this term occurred in the current doc
    int[] lastOffsets;                                 // Last offset we saw
    int[] lastPositions;                               // Last position where this term occurred

    @Override
    ParallelPostingsArray newInstance(int size) {
      return new TermVectorsPostingsArray(size);
    }

    @Override
    void copyTo(ParallelPostingsArray toArray, int numToCopy) {
      assert toArray instanceof TermVectorsPostingsArray;
      TermVectorsPostingsArray to = (TermVectorsPostingsArray) toArray;

      super.copyTo(toArray, numToCopy);

      System.arraycopy(freqs, 0, to.freqs, 0, size);
      System.arraycopy(lastOffsets, 0, to.lastOffsets, 0, size);
      System.arraycopy(lastPositions, 0, to.lastPositions, 0, size);
    }

    @Override
    int bytesPerPosting() {
      return super.bytesPerPosting() + 3 * Integer.BYTES;
    }
  }
}
