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

import org.apache.lucene.analysis.tokenattributes.TermFrequencyAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.ByteBlockPool;
import org.apache.lucene.util.BytesRefHash.BytesStartArray;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.IntBlockPool;

abstract class TermsHashPerField implements Comparable<TermsHashPerField> {
  private static final int HASH_INIT_SIZE = 4;

  final TermsHash termsHash;

  final TermsHashPerField nextPerField;
  protected final DocumentsWriterPerThread.DocState docState;
  protected final FieldInvertState fieldState;
  TermToBytesRefAttribute termAtt;
  protected TermFrequencyAttribute termFreqAtt;

  // Copied from our perThread
  final IntBlockPool intPool;
  final ByteBlockPool bytePool;
  final ByteBlockPool termBytePool;

  final int streamCount;
  final int numPostingInt;

  protected final FieldInfo fieldInfo;

  final BytesRefHash bytesHash;

  ParallelPostingsArray postingsArray;
  private final Counter bytesUsed;

  /** streamCount: how many streams this field stores per term.
   * E.g. doc(+freq) is 1 stream, prox+offset is a second. */

  public TermsHashPerField(int streamCount, FieldInvertState fieldState, TermsHash termsHash, TermsHashPerField nextPerField, FieldInfo fieldInfo) {
    intPool = termsHash.intPool;
    bytePool = termsHash.bytePool;
    termBytePool = termsHash.termBytePool;
    docState = termsHash.docState;
    this.termsHash = termsHash;
    bytesUsed = termsHash.bytesUsed;
    this.fieldState = fieldState;
    this.streamCount = streamCount;
    numPostingInt = 2*streamCount;
    this.fieldInfo = fieldInfo;
    this.nextPerField = nextPerField;
    PostingsBytesStartArray byteStarts = new PostingsBytesStartArray(this, bytesUsed);
    bytesHash = new BytesRefHash(termBytePool, HASH_INIT_SIZE, byteStarts);
  }

  void reset() {
    bytesHash.clear(false);
    if (nextPerField != null) {
      nextPerField.reset();
    }
  }

  public void initReader(ByteSliceReader reader, int termID, int stream) {
    assert stream < streamCount;
    int intStart = postingsArray.intStarts[termID];
    final int[] ints = intPool.buffers[intStart >> IntBlockPool.INT_BLOCK_SHIFT];
    final int upto = intStart & IntBlockPool.INT_BLOCK_MASK;
    reader.init(bytePool,
                postingsArray.byteStarts[termID]+stream*ByteBlockPool.FIRST_LEVEL_SIZE,
                ints[upto+stream]);
  }

  int[] sortedTermIDs;

  /** Collapse the hash table and sort in-place; also sets
   * this.sortedTermIDs to the results */
  public int[] sortPostings() {
    sortedTermIDs = bytesHash.sort();
    return sortedTermIDs;
  }

  private boolean doNextCall;

  // Secondary entry point (for 2nd & subsequent TermsHash),
  // because token text has already been "interned" into
  // textStart, so we hash by textStart.  term vectors use
  // this API.
  public void add(int textStart) throws IOException {
    int termID = bytesHash.addByPoolOffset(textStart);
    if (termID >= 0) {      // New posting
      // First time we are seeing this token since we last
      // flushed the hash.
      // Init stream slices
      if (numPostingInt + intPool.intUpto > IntBlockPool.INT_BLOCK_SIZE) {
        intPool.nextBuffer();
      }

      if (ByteBlockPool.BYTE_BLOCK_SIZE - bytePool.byteUpto < numPostingInt*ByteBlockPool.FIRST_LEVEL_SIZE) {
        bytePool.nextBuffer();
      }

      intUptos = intPool.buffer;
      intUptoStart = intPool.intUpto;
      intPool.intUpto += streamCount;

      // intUptoStart + intPool.intOffset的和值就是在IntBlockPool对象的二位数组中的起始位置
      postingsArray.intStarts[termID] = intUptoStart + intPool.intOffset;

      for(int i=0;i<streamCount;i++) {
        final int upto = bytePool.newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
        intUptos[intUptoStart+i] = upto + bytePool.byteOffset;
      }
      postingsArray.byteStarts[termID] = intUptos[intUptoStart];

      newTerm(termID);

    } else {
      termID = (-termID)-1;
      int intStart = postingsArray.intStarts[termID];
      intUptos = intPool.buffers[intStart >> IntBlockPool.INT_BLOCK_SHIFT];
      intUptoStart = intStart & IntBlockPool.INT_BLOCK_MASK;
      addTerm(termID);
    }
  }

  /** Called once per inverted token.  This is the primary
   *  entry point (for first TermsHash); postings use this
   *  API. */
  void add() throws IOException {
    // We are first in the chain so we must "intern" the
    // term text into textStart address
    // Get the text & hash of this term.
    int termID = bytesHash.add(termAtt.getBytesRef());
      
    //System.out.println("add term=" + termBytesRef.utf8ToString() + " doc=" + docState.docID + " termID=" + termID);

    // termID大于0说明这个term第一次处理
    if (termID >= 0) {// New posting
      bytesHash.byteStart(termID);
      // Init stream slices
      if (numPostingInt + intPool.intUpto > IntBlockPool.INT_BLOCK_SIZE) {
        intPool.nextBuffer();
      }

      if (ByteBlockPool.BYTE_BLOCK_SIZE - bytePool.byteUpto < numPostingInt*ByteBlockPool.FIRST_LEVEL_SIZE) {
        bytePool.nextBuffer();
      }

      // 获得当前正在使用的IntBlockPool中的head buffer
      intUptos = intPool.buffer;
      // 获得下一个head buffer可以写入的位置
      intUptoStart = intPool.intUpto;
      // 预分配streamCount个大小的位置，用来写入数据
      intPool.intUpto += streamCount;

      // intUptoStart + intPool.intOffset的和值就是在IntBlockPool对象的二位数组buffers[]中的起始位置
      postingsArray.intStarts[termID] = intUptoStart + intPool.intOffset;

      for(int i=0;i<streamCount;i++) {
        // term第一次处理时，在ByteBlockPool的二维数组中分配5个数组元素大小的空间
        // 返回值是ByteBlockPool的head buffer中的下一个可以使用的位置(head buffer的下标值)
        final int upto = bytePool.newSlice(ByteBlockPool.FIRST_LEVEL_SIZE);
        // 将得到的可以使用的位置(下标值)存储到IntBlockPool的二维数组中
        intUptos[intUptoStart+i] = upto + bytePool.byteOffset;
      }
      // 上面的for循环结束后，IntBlockPool的二维数组会分配两个连续的空间
      // 存放的两个值描述的是在ByteBlockPool的二维数组中可以使用的位置

      // 刚才IntBlockPool的二维数组
      postingsArray.byteStarts[termID] = intUptos[intUptoStart];

      newTerm(termID);

    } else {
      termID = (-termID)-1;
      // 取出在IntBlockPool中保存这个term信息的位置
      int intStart = postingsArray.intStarts[termID];
      // 取出存储信息的IntBlockPool的buffer
      intUptos = intPool.buffers[intStart >> IntBlockPool.INT_BLOCK_SHIFT];
      intUptoStart = intStart & IntBlockPool.INT_BLOCK_MASK;
      addTerm(termID);
    }

    if (doNextCall) {
      nextPerField.add(postingsArray.textStarts[termID]);
    }
  }

  // 当前正在使用的IntBlockPool中的head buffer, 它记录了ByteBlockPool中的head buffer可以使用的位置
  int[] intUptos;
  int intUptoStart;

  void writeByte(int stream, byte b) {
    // 从IntBlockPool的head buffer中取出一个值，这个值描述了在ByteBlockPool的二维数组中可以写入的位置
    int upto = intUptos[intUptoStart+stream];
    // 取出ByteBlockPool中的head buffer
    byte[] bytes = bytePool.buffers[upto >> ByteBlockPool.BYTE_BLOCK_SHIFT];
    assert bytes != null;
    // 计算在head buffer中的偏移位置
    int offset = upto & ByteBlockPool.BYTE_BLOCK_MASK;
    if (bytes[offset] != 0) {
      // End of slice; allocate a new one
      offset = bytePool.allocSlice(bytes, offset);
      bytes = bytePool.buffer;
      intUptos[intUptoStart+stream] = offset + bytePool.byteOffset;
    }
    bytes[offset] = b;
    // 更新IntBlockPool的head buffer中的值，再次说明，这个值描述了在ByteBlockPool中二维数组中可以写入的位置
    (intUptos[intUptoStart+stream])++;
  }

  public void writeBytes(int stream, byte[] b, int offset, int len) {
    // TODO: optimize
    final int end = offset + len;
    for(int i=offset;i<end;i++)
      writeByte(stream, b[i]);
  }

  // stream的值只有0或1，IntBlockPool的head buffer中对于每一个term都会连续保存两个值
  // 两个值描述了ByteBlockPool的head buffer中的可以写入的两个位置
  // stream的0跟1与这两个位置一一对应
  void writeVInt(int stream, int i) {
    assert stream < streamCount;
    // 将原本需要4个字节存储的int类型，转化成至少1个字节存储
    while ((i & ~0x7F) != 0) {
      writeByte(stream, (byte)((i & 0x7f) | 0x80));
      i >>>= 7;
    }
    writeByte(stream, (byte) i);
  }

  private static final class PostingsBytesStartArray extends BytesStartArray {

    private final TermsHashPerField perField;
    private final Counter bytesUsed;

    private PostingsBytesStartArray(
        TermsHashPerField perField, Counter bytesUsed) {
      this.perField = perField;
      this.bytesUsed = bytesUsed;
    }

    @Override
    public int[] init() {
      if (perField.postingsArray == null) {
        perField.postingsArray = perField.createPostingsArray(2);
        perField.newPostingsArray();
        bytesUsed.addAndGet(perField.postingsArray.size * perField.postingsArray.bytesPerPosting());
      }
      return perField.postingsArray.textStarts;
    }

    @Override
    public int[] grow() {
      ParallelPostingsArray postingsArray = perField.postingsArray;
      final int oldSize = perField.postingsArray.size;
      postingsArray = perField.postingsArray = postingsArray.grow();
      perField.newPostingsArray();
      bytesUsed.addAndGet((postingsArray.bytesPerPosting() * (postingsArray.size - oldSize)));
      return postingsArray.textStarts;
    }

    @Override
    public int[] clear() {
      if (perField.postingsArray != null) {
        bytesUsed.addAndGet(-(perField.postingsArray.size * perField.postingsArray.bytesPerPosting()));
        perField.postingsArray = null;
        perField.newPostingsArray();
      }
      return null;
    }

    @Override
    public Counter bytesUsed() {
      return bytesUsed;
    }
  }

  @Override
  public int compareTo(TermsHashPerField other) {
    return fieldInfo.name.compareTo(other.fieldInfo.name);
  }

  /** Finish adding all instances of this field to the
   *  current document. */
  void finish() throws IOException {
    if (nextPerField != null) {
      nextPerField.finish();
    }
  }

  /** Start adding a new field instance; first is true if
   *  this is the first time this field name was seen in the
   *  document. */
  boolean start(IndexableField field, boolean first) {
    termAtt = fieldState.termAttribute;
    termFreqAtt = fieldState.termFreqAttribute;
    if (nextPerField != null) {
      doNextCall = nextPerField.start(field, first);
    }

    return true;
  }

  /** Called when a term is seen for the first time. */
  abstract void newTerm(int termID) throws IOException;

  /** Called when a previously seen term is seen again. */
  abstract void addTerm(int termID) throws IOException;

  /** Called when the postings array is initialized or
   *  resized. */
  abstract void newPostingsArray();

  /** Creates a new postings array of the specified size. */
  abstract ParallelPostingsArray createPostingsArray(int size);
}
