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
import org.apache.lucene.util.BytesRef;

// TODO: break into separate freq and prox writers as
// codecs; make separate container (tii/tis/skip/*) that can
// be configured as any number of files 1..N
final class FreqProxTermsWriterPerField extends TermsHashPerField {

  private FreqProxPostingsArray freqProxPostingsArray;

  // 词频
  final boolean hasFreq;
  // 位置
  final boolean hasProx;
  // 偏移
  final boolean hasOffsets;
  PayloadAttribute payloadAttribute;
  OffsetAttribute offsetAttribute;
  long sumTotalTermFreq;
  long sumDocFreq;

  // How many docs have this field:
  int docCount;

  /** Set to true if any token had a payload in the current
   *  segment. */
  boolean sawPayloads;

  public FreqProxTermsWriterPerField(FieldInvertState invertState, TermsHash termsHash, FieldInfo fieldInfo, TermsHashPerField nextPerField) {
    super(fieldInfo.getIndexOptions().compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0 ? 2 : 1, invertState, termsHash, nextPerField, fieldInfo);
    IndexOptions indexOptions = fieldInfo.getIndexOptions();
    assert indexOptions != IndexOptions.NONE;
    hasFreq = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
    hasProx = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    hasOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
  }

  @Override
  void finish() throws IOException {
    super.finish();
    // 所有文档的相同域名中不同term种类的个数(去重)的总和, 比如文档1跟文档2中有相同的域名为"content", sumDocFreq的值为两个文档中uniqueTermCount的和
    sumDocFreq += fieldState.uniqueTermCount;
    // 所有文档的相同的域名的所有term的总个数(非去重)
    sumTotalTermFreq += fieldState.length;
    if (fieldState.length > 0) {
      // docCount表示都多少个doc包含这个域
      docCount++;
    }

    if (sawPayloads) {
      fieldInfo.setStorePayloads();
    }
  }

  @Override
  boolean start(IndexableField f, boolean first) {
    super.start(f, first);
    payloadAttribute = fieldState.payloadAttribute;
    offsetAttribute = fieldState.offsetAttribute;
    return true;
  }

  void writeProx(int termID, int proxCode) {
    if (payloadAttribute == null) {
      writeVInt(1, proxCode<<1);
    } else {
      BytesRef payload = payloadAttribute.getPayload();
      if (payload != null && payload.length > 0) {
        writeVInt(1, (proxCode<<1)|1);
        writeVInt(1, payload.length);
        writeBytes(1, payload.bytes, payload.offset, payload.length);
        sawPayloads = true;
      } else {
        writeVInt(1, proxCode<<1);
      }
    }

    assert postingsArray == freqProxPostingsArray;
    // 记录当前处理的term在文档中的位置
    freqProxPostingsArray.lastPositions[termID] = fieldState.position;
  }

  void writeOffsets(int termID, int offsetAccum) {
    final int startOffset = offsetAccum + offsetAttribute.startOffset();
    final int endOffset = offsetAccum + offsetAttribute.endOffset();
    assert startOffset - freqProxPostingsArray.lastOffsets[termID] >= 0;
    writeVInt(1, startOffset - freqProxPostingsArray.lastOffsets[termID]);
    writeVInt(1, endOffset - startOffset);
    freqProxPostingsArray.lastOffsets[termID] = startOffset;
  }

  @Override
  // term第一次出现
  void newTerm(final int termID) {
    // First time we're seeing this term since the last
    // flush
    final FreqProxPostingsArray postings = freqProxPostingsArray;

    // 记录当前处理的文档号, 后面会用来判断一个文档是否处理结束
    postings.lastDocIDs[termID] = docState.docID;
    if (!hasFreq) {
      assert postings.termFreqs == null;
      postings.lastDocCodes[termID] = docState.docID;
      fieldState.maxTermFrequency = Math.max(1, fieldState.maxTermFrequency);
    } else {
        //记录经过编码后的文档号，左移一位的原因看 lastDocCodes[]数组的注释
      postings.lastDocCodes[termID] = docState.docID << 1;
      postings.termFreqs[termID] = getTermFreq();
      if (hasProx) {
        // 写入term在文档中的位置信息
        writeProx(termID, fieldState.position);
        // 写入term在文档中的偏移信息
        if (hasOffsets) {
          writeOffsets(termID, fieldState.offset);
        }
      } else {
        assert !hasOffsets;
      }
      fieldState.maxTermFrequency = Math.max(postings.termFreqs[termID], fieldState.maxTermFrequency);
    }
    // 更新当前域包含的term种类数
    fieldState.uniqueTermCount++;
  }

  @Override
  // 处理之前出现过的term
  void addTerm(final int termID) {
    final FreqProxPostingsArray postings = freqProxPostingsArray;
    assert !hasFreq || postings.termFreqs[termID] > 0;

    if (!hasFreq) {
      assert postings.termFreqs == null;
      if (termFreqAtt.getTermFrequency() != 1) {
        throw new IllegalStateException("field \"" + fieldInfo.name + "\": must index term freq while using custom TermFrequencyAttribute");
      }
      if (docState.docID != postings.lastDocIDs[termID]) {
        // New document; now encode docCode for previous doc:
        assert docState.docID > postings.lastDocIDs[termID];
        writeVInt(0, postings.lastDocCodes[termID]);
        postings.lastDocCodes[termID] = docState.docID - postings.lastDocIDs[termID];
        postings.lastDocIDs[termID] = docState.docID;
        fieldState.uniqueTermCount++;
      }
      // if语句为真：当前处理的term是另外一篇文档(说明上篇文档处理结束了), 需要对上一遍文档的处理进行收尾工作
    } else if (docState.docID != postings.lastDocIDs[termID]) {
      assert docState.docID > postings.lastDocIDs[termID]:"id: "+docState.docID + " postings ID: "+ postings.lastDocIDs[termID] + " termID: "+termID;
      // Term not yet seen in the current doc but previously
      // seen in other doc(s) since the last flush

      // Now that we know doc freq for previous doc,
      // write it & lastDocCode
      // 运行到这里，说明我们当前term在新的文档中第一次出现, 并且这个term肯定在上个文档中出现
      // 那么这时候就可以将这个term在上篇文档中的词频写入到倒排表中了
      if (1 == postings.termFreqs[termID]) {
          // 如果term的词频只有1，跟lastDocCode值计算后储存，这里的与操作解释了为什么文档号时执行 docId << 1的操作
        writeVInt(0, postings.lastDocCodes[termID]|1);
      } else {
        // 两个Vint存储编码后的文档号跟frep, 在读取的时候，赶紧编码后的文档号的最后一个bit是否为0来确定 docId跟freq的读取方式
        writeVInt(0, postings.lastDocCodes[termID]);
        writeVInt(0, postings.termFreqs[termID]);
      }

      // Init freq for the current document
      // 记录当前文档中term的词频
      postings.termFreqs[termID] = getTermFreq();
      fieldState.maxTermFrequency = Math.max(postings.termFreqs[termID], fieldState.maxTermFrequency);
      // 计算文档号的差值，同样的左移一位后的值赋值给lastDocCodes[]数组
      postings.lastDocCodes[termID] = (docState.docID - postings.lastDocIDs[termID]) << 1;
      // 记录当前的文档号, 用来判断当前文档是否处理结束
      postings.lastDocIDs[termID] = docState.docID;
      if (hasProx) {
        // 写入term在当前文档的位置信息
        writeProx(termID, fieldState.position);
        if (hasOffsets) {
          postings.lastOffsets[termID] = 0;
          writeOffsets(termID, fieldState.offset);
        }
      } else {
        assert !hasOffsets;
      }
      fieldState.uniqueTermCount++;
      // 说明当前term在当前文档中又一次出现了，那么只要统计它的位置信息就行了
    } else {
      // 更新term在当前文档中的词频
      postings.termFreqs[termID] = Math.addExact(postings.termFreqs[termID], getTermFreq());
      // 更新这个域包含的term的个数
      fieldState.maxTermFrequency = Math.max(fieldState.maxTermFrequency, postings.termFreqs[termID]);
      if (hasProx) {
        // 将term的位置信息写到倒排表中, 注意位置信息的值是一个差值, 跟上一个这个term的在文档中位置的差值
        // 所以为什么要有lastPositions[]数组
        // lastPositions[]数组用来保存term上次在文档中的位置
        writeProx(termID, fieldState.position-postings.lastPositions[termID]);
        if (hasOffsets) {
          writeOffsets(termID, fieldState.offset);
        }
      }
    }
  }

  private int getTermFreq() {
    //
    int freq = termFreqAtt.getTermFrequency();
    if (freq != 1) {
      if (hasProx) {
        throw new IllegalStateException("field \"" + fieldInfo.name + "\": cannot index positions while using custom TermFrequencyAttribute");
      }
    }

    return freq;
  }

  @Override
  public void newPostingsArray() {
    freqProxPostingsArray = (FreqProxPostingsArray) postingsArray;
  }

  @Override
  ParallelPostingsArray createPostingsArray(int size) {
    IndexOptions indexOptions = fieldInfo.getIndexOptions();
    assert indexOptions != IndexOptions.NONE;
    boolean hasFreq = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
    boolean hasProx = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    boolean hasOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;
    return new FreqProxPostingsArray(size, hasFreq, hasProx, hasOffsets);
  }

  static final class FreqProxPostingsArray extends ParallelPostingsArray {
    public FreqProxPostingsArray(int size, boolean writeFreqs, boolean writeProx, boolean writeOffsets) {
      super(size);
      if (writeFreqs) {
        termFreqs = new int[size];
      }
      lastDocIDs = new int[size];
      lastDocCodes = new int[size];
      if (writeProx) {
        lastPositions = new int[size];
        if (writeOffsets) {
          lastOffsets = new int[size];
        }
      } else {
        assert !writeOffsets;
      }
      //System.out.println("PA init freqs=" + writeFreqs + " pos=" + writeProx + " offs=" + writeOffsets);
    }

    /**下面几个数组的下标都是termID*/
    // 目前term当前文档中出现的次数(这个值不是term在当前文档中最终的词频)
    int termFreqs[];                                   // # times this term occurs in the current doc
    // 存放term上次出现文档号,该文档号未被编码
    // 用来判断上篇文档是否已经处理结束
    // 当一篇文档已经处理结束，我们才能统计term在这篇文档中的词频
    int lastDocIDs[];                                  // Last docID where this term occurred
    // 文档号(差值)左移一位的值
    int lastDocCodes[];                                // Code for prior doc
    // 记录term在文档中最后出现的位置
    // 在存放同一篇文档的相同term时候计算两个term之间的位置差值，并且记录这个差值
    // 即位置信息是差值存储
    int lastPositions[];                               // Last position where this term occurred

    int lastOffsets[];                                 // Last endOffset where this term occurred

    @Override
    ParallelPostingsArray newInstance(int size) {
      return new FreqProxPostingsArray(size, termFreqs != null, lastPositions != null, lastOffsets != null);
    }

    @Override
    void copyTo(ParallelPostingsArray toArray, int numToCopy) {
      assert toArray instanceof FreqProxPostingsArray;
      FreqProxPostingsArray to = (FreqProxPostingsArray) toArray;

      super.copyTo(toArray, numToCopy);

      System.arraycopy(lastDocIDs, 0, to.lastDocIDs, 0, numToCopy);
      System.arraycopy(lastDocCodes, 0, to.lastDocCodes, 0, numToCopy);
      if (lastPositions != null) {
        assert to.lastPositions != null;
        System.arraycopy(lastPositions, 0, to.lastPositions, 0, numToCopy);
      }
      if (lastOffsets != null) {
        assert to.lastOffsets != null;
        System.arraycopy(lastOffsets, 0, to.lastOffsets, 0, numToCopy);
      }
      if (termFreqs != null) {
        assert to.termFreqs != null;
        System.arraycopy(termFreqs, 0, to.termFreqs, 0, numToCopy);
      }
    }

    @Override
    int bytesPerPosting() {
      int bytes = ParallelPostingsArray.BYTES_PER_POSTING + 2 * Integer.BYTES;
      if (lastPositions != null) {
        bytes += Integer.BYTES;
      }
      if (lastOffsets != null) {
        bytes += Integer.BYTES;
      }
      if (termFreqs != null) {
        bytes += Integer.BYTES;
      }

      return bytes;
    }
  }
}
