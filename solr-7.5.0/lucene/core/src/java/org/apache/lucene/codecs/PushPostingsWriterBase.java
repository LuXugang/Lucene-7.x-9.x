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
package org.apache.lucene.codecs;


import java.io.IOException;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;

/**
 * Extension of {@link PostingsWriterBase}, adding a push
 * API for writing each element of the postings.  This API
 * is somewhat analagous to an XML SAX API, while {@link
 * PostingsWriterBase} is more like an XML DOM API.
 * 
 * @see PostingsReaderBase
 * @lucene.experimental
 */
// TODO: find a better name; this defines the API that the
// terms dict impls use to talk to a postings impl.
// TermsDict + PostingsReader/WriterBase == PostingsConsumer/Producer
public abstract class PushPostingsWriterBase extends PostingsWriterBase {

  // Reused in writeTerm
  private PostingsEnum postingsEnum;
  private int enumFlags;

  /** {@link FieldInfo} of current field being written. */
  protected FieldInfo fieldInfo;

  /** {@link IndexOptions} of current field being
      written */
  protected IndexOptions indexOptions;

  /** True if the current field writes freqs. */
  protected boolean writeFreqs;

  /** True if the current field writes positions. */
  protected boolean writePositions;

  /** True if the current field writes payloads. */
  protected boolean writePayloads;

  /** True if the current field writes offsets. */
  protected boolean writeOffsets;

  /** Sole constructor. (For invocation by subclass 
   *  constructors, typically implicit.) */
  protected PushPostingsWriterBase() {
  }

  /** Return a newly created empty TermState */
  public abstract BlockTermState newTermState() throws IOException;

  /** Start a new term.  Note that a matching call to {@link
   *  #finishTerm(BlockTermState)} is done, only if the term has at least one
   *  document. */
  public abstract void startTerm() throws IOException;

  /** Finishes the current term.  The provided {@link
   *  BlockTermState} contains the term's summary statistics, 
   *  and will holds metadata from PBF when returned */
  public abstract void finishTerm(BlockTermState state) throws IOException;

  /** 
   * Sets the current field for writing, and returns the
   * fixed length of long[] metadata (which is fixed per
   * field), called when the writing switches to another field. */
  @Override
  public int setField(FieldInfo fieldInfo) {
    this.fieldInfo = fieldInfo;
    indexOptions = fieldInfo.getIndexOptions();

    writeFreqs = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS) >= 0;
    writePositions = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS) >= 0;
    writeOffsets = indexOptions.compareTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS) >= 0;        
    writePayloads = fieldInfo.hasPayloads();

    if (writeFreqs == false) {
      enumFlags = 0;
    } else if (writePositions == false) {
      enumFlags = PostingsEnum.FREQS;
    } else if (writeOffsets == false) {
      if (writePayloads) {
        enumFlags = PostingsEnum.PAYLOADS;
      } else {
        enumFlags = PostingsEnum.POSITIONS;
      }
    } else {
      if (writePayloads) {
        enumFlags = PostingsEnum.PAYLOADS | PostingsEnum.OFFSETS;
      } else {
        enumFlags = PostingsEnum.OFFSETS;
      }
    }
    return 0;
  }

  @Override
  public final BlockTermState writeTerm(BytesRef term, TermsEnum termsEnum, FixedBitSet docsSeen) throws IOException {
    startTerm();
    // 生成一个可以读取某个term所有倒排表信息的PostingsEnum对象
    postingsEnum = termsEnum.postings(postingsEnum, enumFlags);
    assert postingsEnum != null;

    int docFreq = 0;
    long totalTermFreq = 0;
    // 遍历包含当前term的每一篇文档，统计当前term在每一篇文档中的docId、frequency，position、payload、offset信息
    while (true) {
      int docID = postingsEnum.nextDoc();
      if (docID == PostingsEnum.NO_MORE_DOCS) {
        break;
      }
      docFreq++;
      docsSeen.set(docID);
      int freq;
      if (writeFreqs) {
        // 获得词频，在调用postingsEnum.nextDoc()的过程就同时获得了词频
        freq = postingsEnum.freq();
        totalTermFreq += freq;
      } else {
        freq = -1;
      }
      // 处理docID跟frequency
      startDoc(docID, freq);

      if (writePositions) {
        // term的词频为freq表示有freq个不同的位置
        for(int i=0;i<freq;i++) {
          int pos = postingsEnum.nextPosition();
          BytesRef payload = writePayloads ? postingsEnum.getPayload() : null;
          int startOffset;
          int endOffset;
          if (writeOffsets) {
            startOffset = postingsEnum.startOffset();
            endOffset = postingsEnum.endOffset();
          } else {
            startOffset = -1;
            endOffset = -1;
          }
          // 处理position、payload、offset
          addPosition(pos, payload, startOffset, endOffset);
        }
      }
      // 包含当前term的某一篇文档的收尾工作
      // 当前处理完term在当前文档中位置跟payload信息时调用此方法。
      finishDoc();
    }

    if (docFreq == 0) {
      return null;
    } else {
      BlockTermState state = newTermState();
      // docFreq为包含当前term的文档数量总和
      state.docFreq = docFreq;
      // totalTermFreq为当前term的在所有文档中的词频总和
      state.totalTermFreq = writeFreqs ? totalTermFreq : -1;
      finishTerm(state);
      return state;
    }
  }

  /** Adds a new doc in this term. 
   * <code>freq</code> will be -1 when term frequencies are omitted
   * for the field. */
  public abstract void startDoc(int docID, int freq) throws IOException;

  /** Add a new position and payload, and start/end offset.  A
   *  null payload means no payload; a non-null payload with
   *  zero length also means no payload.  Caller may reuse
   *  the {@link BytesRef} for the payload between calls
   *  (method must fully consume the payload). <code>startOffset</code>
   *  and <code>endOffset</code> will be -1 when offsets are not indexed. */
  public abstract void addPosition(int position, BytesRef payload, int startOffset, int endOffset) throws IOException;

  /** Called when we are done adding positions and payloads
   *  for each doc. */
  public abstract void finishDoc() throws IOException;
}
