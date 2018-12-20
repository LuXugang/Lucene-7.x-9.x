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
package org.apache.lucene.util;


import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.util.ByteBlockPool.DirectAllocator;

import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_MASK;
import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_SHIFT;
import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_SIZE;

/**
 * {@link BytesRefHash} is a special purpose hash-map like data-structure
 * optimized for {@link BytesRef} instances. BytesRefHash maintains mappings of
 * byte arrays to ids (Map&lt;BytesRef,int&gt;) storing the hashed bytes
 * efficiently in continuous storage. The mapping to the id is
 * encapsulated inside {@link BytesRefHash} and is guaranteed to be increased
 * for each added {@link BytesRef}.
 * 
 * <p>
 * Note: The maximum capacity {@link BytesRef} instance passed to
 * {@link #add(BytesRef)} must not be longer than {@link ByteBlockPool#BYTE_BLOCK_SIZE}-2. 
 * The internal storage is limited to 2GB total byte storage.
 * </p>
 * 
 * @lucene.internal
 */
public final class BytesRefHash {

  public static final int DEFAULT_CAPACITY = 16;

  // the following fields are needed by comparator,
  // so package private to prevent access$-methods:
  final ByteBlockPool pool;
  // 数组下标值是ord(termId)，数组元素是在 ByteBlockPool对象的buffers[][]二维数组中的起始位置
  int[] bytesStart;

  private final BytesRef scratch1 = new BytesRef();
  private int hashSize;
  private int hashHalfSize;
  private int hashMask;
  private int count;
  private int lastCount = -1;
  // 数组的下标是域值的Hash值，而数组中的元素(即是termId)是数组byteStart的下标值，而这个下标值对应的数组元素是 二维数组bytes[][]的起始位置
  private int[] ids;
  private final BytesStartArray bytesStartArray;
  private Counter bytesUsed;

  /**
   * Creates a new {@link BytesRefHash} with a {@link ByteBlockPool} using a
   * {@link DirectAllocator}.
   */
  public BytesRefHash() { 
    this(new ByteBlockPool(new DirectAllocator()));
  }
  
  /**
   * Creates a new {@link BytesRefHash}
   */
  public BytesRefHash(ByteBlockPool pool) {
    this(pool, DEFAULT_CAPACITY, new DirectBytesStartArray(DEFAULT_CAPACITY));
  }

  /**
   * Creates a new {@link BytesRefHash}
   */
  public BytesRefHash(ByteBlockPool pool, int capacity, BytesStartArray bytesStartArray) {
    hashSize = capacity;
    hashHalfSize = hashSize >> 1;
    hashMask = hashSize - 1;
    this.pool = pool;
    ids = new int[hashSize];
    Arrays.fill(ids, -1);
    this.bytesStartArray = bytesStartArray;
    bytesStart = bytesStartArray.init();
    bytesUsed = bytesStartArray.bytesUsed() == null? Counter.newCounter() : bytesStartArray.bytesUsed();
    bytesUsed.addAndGet(hashSize * Integer.BYTES);
  }

  /**
   * Returns the number of {@link BytesRef} values in this {@link BytesRefHash}.
   * 
   * @return the number of {@link BytesRef} values in this {@link BytesRefHash}.
   */
  public int size() {
    return count;
  }

  /**
   * Populates and returns a {@link BytesRef} with the bytes for the given
   * bytesID.
   * <p>
   * Note: the given bytesID must be a positive integer less than the current
   * size ({@link #size()})
   * 
   * @param bytesID
   *          the id
   * @param ref
   *          the {@link BytesRef} to populate
   * 
   * @return the given BytesRef instance populated with the bytes for the given
   *         bytesID
   */
  public BytesRef get(int bytesID, BytesRef ref) {
    assert bytesStart != null : "bytesStart is null - not initialized";
    assert bytesID < bytesStart.length: "bytesID exceeds byteStart len: " + bytesStart.length;
    // 根据ord值(termID)，从bytesStart[]数组中取出在 二维数组中的位置，然后取出ord值对应的term值(ByteRef对象封装)
    pool.setBytesRef(ref, bytesStart[bytesID]);
    return ref;
  }

  /**
   * Returns the ids array in arbitrary order. Valid ids start at offset of 0
   * and end at a limit of {@link #size()} - 1
   * <p>
   * Note: This is a destructive operation. {@link #clear()} must be called in
   * order to reuse this {@link BytesRefHash} instance.
   * </p>
   *
   * @lucene.internal
   */
  // 将分散在数组不同位置的元素都放到数组最前面去
  public int[] compact() {
    assert bytesStart != null : "bytesStart is null - not initialized";
    // upto记录了数组从下标0开始第一个为-1(非法值)的下标, 用于遍历过程中当ids[i]为合法值(termID)时,跟ids[upto]进行交换
    int upto = 0;
    for (int i = 0; i < hashSize; i++) {
      // if语句为true：说明当前元素是合法的值(termID)
      if (ids[i] != -1) {
          // if语句为true：那么交换
        if (upto < i) {
          ids[upto] = ids[i];
          ids[i] = -1;
        }
        upto++;
      }
    }

    assert upto == count;
    lastCount = count;
    return ids;
  }

  /**
   * Returns the values array sorted by the referenced byte values.
   * <p>
   * Note: This is a destructive operation. {@link #clear()} must be called in
   * order to reuse this {@link BytesRefHash} instance.
   * </p>
   */
  public int[] sort() {
     // 将ids[]中稀疏的元素都放到数组最前面去
    final int[] compact = compact();
    new StringMSBRadixSorter() {

      BytesRef scratch = new BytesRef();

      @Override
      protected void swap(int i, int j) {
        int tmp = compact[i];
        compact[i] = compact[j];
        compact[j] = tmp;
      }

      @Override
      protected BytesRef get(int i) {
        pool.setBytesRef(scratch, bytesStart[compact[i]]);
        return scratch;
      }

    }.sort(0, count);
    return compact;
  }

  private boolean equals(int id, BytesRef b) {
    pool.setBytesRef(scratch1, bytesStart[id]);
    return scratch1.bytesEquals(b);
  }

  private boolean shrink(int targetSize) {
    // Cannot use ArrayUtil.shrink because we require power
    // of 2:
    int newSize = hashSize;
    while (newSize >= 8 && newSize / 4 > targetSize) {
      newSize /= 2;
    }
    if (newSize != hashSize) {
      bytesUsed.addAndGet(Integer.BYTES * -(hashSize - newSize));
      hashSize = newSize;
      ids = new int[hashSize];
      Arrays.fill(ids, -1);
      hashHalfSize = newSize / 2;
      hashMask = newSize - 1;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Clears the {@link BytesRef} which maps to the given {@link BytesRef}
   */
  public void clear(boolean resetPool) {
    lastCount = count;
    count = 0;
    if (resetPool) {
      pool.reset(false, false); // we don't need to 0-fill the buffers
    }
    bytesStart = bytesStartArray.clear();
    if (lastCount != -1 && shrink(lastCount)) {
      // shrink clears the hash entries
      return;
    }
    Arrays.fill(ids, -1);
  }

  public void clear() {
    clear(true);
  }
  
  /**
   * Closes the BytesRefHash and releases all internally used memory
   */
  public void close() {
    clear(true);
    ids = null;
    bytesUsed.addAndGet(Integer.BYTES * -hashSize);
  }

  /**
   * Adds a new {@link BytesRef}
   * 
   * @param bytes
   *          the bytes to hash
   * @return the id the given bytes are hashed if there was no mapping for the
   *         given bytes, otherwise <code>(-(id)-1)</code>. This guarantees
   *         that the return value will always be &gt;= 0 if the given bytes
   *         haven't been hashed before.
   * 
   * @throws MaxBytesLengthExceededException
   *           if the given bytes are {@code > 2 +}
   *           {@link ByteBlockPool#BYTE_BLOCK_SIZE}
   */
  public int add(BytesRef bytes) {
    assert bytesStart != null : "Bytesstart is null - not initialized";
    final int length = bytes.length;
    // final position
    final int hashPos = findHash(bytes);
    int e = ids[hashPos];

    // if语句true: ids数组中没有存放过这个bytes, 是一个new entry
    if (e == -1) {
      // new entry
      final int len2 = 2 + bytes.length;
      // BYTE_BLOCK_SIZE的大小是 1 << 15 ;
      // byteUpto表示在head buffer中的位置(用来体现head buffer的使用量)
      if (len2 + pool.byteUpto > BYTE_BLOCK_SIZE) {
          // if语句true: bytes(域值)长度太长了, 超过了一个head buffer的长度,
        if (len2 > BYTE_BLOCK_SIZE) {
          throw new MaxBytesLengthExceededException("bytes can be at most "
              + (BYTE_BLOCK_SIZE - 2) + " in length; got " + bytes.length);
        }
        // 重新分配一个二维数组，并且将旧的二维数组的值赋值到新的二维数组中
        pool.nextBuffer();
      }
      // 获取当前的head buffer
      final byte[] buffer = pool.buffer;
      // bufferUpto用来描述head buffer当前使用量
      final int bufferUpto = pool.byteUpto;
      if (count >= bytesStart.length) {
        bytesStart = bytesStartArray.grow();
        assert count < bytesStart.length + 1 : "count: " + count + " len: "
            + bytesStart.length;
      }
      // count是一个递增的值, 给从未出现过的域值提供一个值，这个值是bytesStart数组的下标，对应的数组元素是在二维数组byte[][]中的起始位置
      e = count++;

      // 数组元素是 二维数组byte[][]中的一个起始位置, 这个二维数组用来存放域值（不会重复存放相同的域值）
        //
      bytesStart[e] = bufferUpto + pool.byteOffset;

      // We first encode the length, followed by the
      // bytes. Length is encoded as vInt, but will consume
      // 1 or 2 bytes at most (we reject too-long terms,
      // above).
      // length描述了用ByteRef对象描述的term需要占用的字节个数
      if (length < 128) {
        // 1 byte to store length
        buffer[bufferUpto] = (byte) length;
        // length + 1表示要占用head buffer的长度，其中1就是存放域值长度值所需要的一个字节大小
        pool.byteUpto += length + 1;
        assert length >= 0: "Length must be positive: " + length;
        // 将用ByteRef对象表示的term值写到buffer[]数组中
        System.arraycopy(bytes.bytes, bytes.offset, buffer, bufferUpto + 1,
            length);
      } else {
        // 2 byte to store length
        buffer[bufferUpto] = (byte) (0x80 | (length & 0x7f));
        buffer[bufferUpto + 1] = (byte) ((length >> 7) & 0xff);
        // length + 2表示要占用head buffer的长度，其中2就是存放域值长度值所需要的一个字节大小,
        // 注意域值(用ByteRef表示)的长度肯定小于 1 << 15（前面已经做了判断）, 所以只要2个字节就能存储
        pool.byteUpto += length + 2;
        System.arraycopy(bytes.bytes, bytes.offset, buffer, bufferUpto + 2,
            length);
      }
      assert ids[hashPos] == -1;
      // ids数组，下标是域值的hash值，而对应的数组元素是在byteStart数组的下标值，通过这个下标对应的数组元素就能找到在byte[][]二维数组的起始位置
      ids[hashPos] = e;

      if (count == hashHalfSize) {
        rehash(2 * hashSize, true);
      }
      return e;
    }
    // 这里上下的两个return分别返回一个负数跟正数，为了区分当前处理的这个域值是不是第一次处理，是的话返回一个正数
    return -(e + 1);
  }
  
  /**
   * Returns the id of the given {@link BytesRef}.
   * 
   * @param bytes
   *          the bytes to look for
   * 
   * @return the id of the given bytes, or {@code -1} if there is no mapping for the
   *         given bytes.
   */
  public int find(BytesRef bytes) {
    return ids[findHash(bytes)];
  }

  private int findHash(BytesRef bytes) {
    assert bytesStart != null : "bytesStart is null - not initialized";

    // 使用murmurhash3_x86_32方法获得Hash值
    int code = doHash(bytes.bytes, bytes.offset, bytes.length);

    // final position
    int hashPos = code & hashMask;
    int e = ids[hashPos];
    if (e != -1 && !equals(e, bytes)) {
      // ids数组中hashPos这个下标已经有一个值并且跟参数bytes的值不一样，说明发生了冲突, 然后使用线性探测法找到一个可用的位置
      // Conflict; use linear probe to find an open slot
      // (see LUCENE-5604):
      do {
        code++;
        hashPos = code & hashMask;
        e = ids[hashPos];
      } while (e != -1 && !equals(e, bytes));
    }
    // 返回-1表示没有在ids数组中找到bytes的
    return hashPos;
  }

  /** Adds a "arbitrary" int offset instead of a BytesRef
   *  term.  This is used in the indexer to hold the hash for term
   *  vectors, because they do not redundantly store the byte[] term
   *  directly and instead reference the byte[] term
   *  already stored by the postings BytesRefHash.  See
   *  add(int textStart) in TermsHashPerField. */
  public int addByPoolOffset(int offset) {
    assert bytesStart != null : "Bytesstart is null - not initialized";
    // final position
    int code = offset;
    int hashPos = offset & hashMask;
    int e = ids[hashPos];
    if (e != -1 && bytesStart[e] != offset) {
      // Conflict; use linear probe to find an open slot
      // (see LUCENE-5604):
      do {
        code++;
        hashPos = code & hashMask;
        e = ids[hashPos];
      } while (e != -1 && bytesStart[e] != offset);
    }
    if (e == -1) {
      // new entry
      if (count >= bytesStart.length) {
        bytesStart = bytesStartArray.grow();
        assert count < bytesStart.length + 1 : "count: " + count + " len: "
            + bytesStart.length;
      }
      e = count++;
      bytesStart[e] = offset;
      assert ids[hashPos] == -1;
      ids[hashPos] = e;

      if (count == hashHalfSize) {
        rehash(2 * hashSize, false);
      }
      return e;
    }
    return -(e + 1);
  }

  /**
   * Called when hash is too small ({@code > 50%} occupied) or too large ({@code < 20%}
   * occupied).
   */
  private void rehash(final int newSize, boolean hashOnData) {
    final int newMask = newSize - 1;
    bytesUsed.addAndGet(Integer.BYTES * (newSize));
    final int[] newHash = new int[newSize];
    Arrays.fill(newHash, -1);
    for (int i = 0; i < hashSize; i++) {
      final int e0 = ids[i];
      if (e0 != -1) {
        int code;
        if (hashOnData) {
          final int off = bytesStart[e0];
          final int start = off & BYTE_BLOCK_MASK;
          final byte[] bytes = pool.buffers[off >> BYTE_BLOCK_SHIFT];
          final int len;
          int pos;
          if ((bytes[start] & 0x80) == 0) {
            // length is 1 byte
            len = bytes[start];
            pos = start + 1;
          } else {
            len = (bytes[start] & 0x7f) + ((bytes[start + 1] & 0xff) << 7);
            pos = start + 2;
          }
          code = doHash(bytes, pos, len);
        } else {
          code = bytesStart[e0];
        }

        int hashPos = code & newMask;
        assert hashPos >= 0;
        if (newHash[hashPos] != -1) {
          // Conflict; use linear probe to find an open slot
          // (see LUCENE-5604):
          do {
            code++;
            hashPos = code & newMask;
          } while (newHash[hashPos] != -1);
        }
        newHash[hashPos] = e0;
      }
    }

    hashMask = newMask;
    bytesUsed.addAndGet(Integer.BYTES * (-ids.length));
    ids = newHash;
    hashSize = newSize;
    hashHalfSize = newSize / 2;
  }

  // TODO: maybe use long?  But our keys are typically short...
  private int doHash(byte[] bytes, int offset, int length) {
    return StringHelper.murmurhash3_x86_32(bytes, offset, length, StringHelper.GOOD_FAST_HASH_SEED);
  }

  /**
   * reinitializes the {@link BytesRefHash} after a previous {@link #clear()}
   * call. If {@link #clear()} has not been called previously this method has no
   * effect.
   */
  public void reinit() {
    if (bytesStart == null) {
      bytesStart = bytesStartArray.init();
    }
    
    if (ids == null) {
      ids = new int[hashSize];
      bytesUsed.addAndGet(Integer.BYTES * hashSize);
    }
  }

  /**
   * Returns the bytesStart offset into the internally used
   * {@link ByteBlockPool} for the given bytesID
   * 
   * @param bytesID
   *          the id to look up
   * @return the bytesStart offset into the internally used
   *         {@link ByteBlockPool} for the given id
   */
  public int byteStart(int bytesID) {
    assert bytesStart != null : "bytesStart is null - not initialized";
    assert bytesID >= 0 && bytesID < count : bytesID;
    return bytesStart[bytesID];
  }

  /**
   * Thrown if a {@link BytesRef} exceeds the {@link BytesRefHash} limit of
   * {@link ByteBlockPool#BYTE_BLOCK_SIZE}-2.
   */
  @SuppressWarnings("serial")
  public static class MaxBytesLengthExceededException extends RuntimeException {
    MaxBytesLengthExceededException(String message) {
      super(message);
    }
  }

  /** Manages allocation of the per-term addresses. */
  public abstract static class BytesStartArray {
    /**
     * Initializes the BytesStartArray. This call will allocate memory
     * 
     * @return the initialized bytes start array
     */
    public abstract int[] init();

    /**
     * Grows the {@link BytesStartArray}
     * 
     * @return the grown array
     */
    public abstract int[] grow();

    /**
     * clears the {@link BytesStartArray} and returns the cleared instance.
     * 
     * @return the cleared instance, this might be <code>null</code>
     */
    public abstract int[] clear();

    /**
     * A {@link Counter} reference holding the number of bytes used by this
     * {@link BytesStartArray}. The {@link BytesRefHash} uses this reference to
     * track it memory usage
     * 
     * @return a {@link AtomicLong} reference holding the number of bytes used
     *         by this {@link BytesStartArray}.
     */
    public abstract Counter bytesUsed();
  }

  /** A simple {@link BytesStartArray} that tracks
   *  memory allocation using a private {@link Counter}
   *  instance.  */
  public static class DirectBytesStartArray extends BytesStartArray {
    // TODO: can't we just merge this w/
    // TrackingDirectBytesStartArray...?  Just add a ctor
    // that makes a private bytesUsed?

    protected final int initSize;
    private int[] bytesStart;
    private final Counter bytesUsed;
    
    public DirectBytesStartArray(int initSize, Counter counter) {
      this.bytesUsed = counter;
      this.initSize = initSize;      
    }
    
    public DirectBytesStartArray(int initSize) {
      this(initSize, Counter.newCounter());
    }

    @Override
    public int[] clear() {
      return bytesStart = null;
    }

    @Override
    public int[] grow() {
      assert bytesStart != null;
      return bytesStart = ArrayUtil.grow(bytesStart, bytesStart.length + 1);
    }

    @Override
    public int[] init() {
      return bytesStart = new int[ArrayUtil.oversize(initSize, Integer.BYTES)];
    }

    @Override
    public Counter bytesUsed() {
      return bytesUsed;
    }
  }
}
