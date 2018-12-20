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

/**
 * A pool for int blocks similar to {@link ByteBlockPool}
 * @lucene.internal
 */
public final class IntBlockPool {
  public final static int INT_BLOCK_SHIFT = 13;
  public final static int INT_BLOCK_SIZE = 1 << INT_BLOCK_SHIFT;
  public final static int INT_BLOCK_MASK = INT_BLOCK_SIZE - 1;
  
  /** Abstract class for allocating and freeing int
   *  blocks. */
  public abstract static class Allocator {
    protected final int blockSize;

    public Allocator(int blockSize) {
      this.blockSize = blockSize;
    }

    public abstract void recycleIntBlocks(int[][] blocks, int start, int end);

    public int[] getIntBlock() {
      return new int[blockSize];
    }
  }
  
  /** A simple {@link Allocator} that never recycles. */
  public static final class DirectAllocator extends Allocator {

    /**
     * Creates a new {@link DirectAllocator} with a default block size
     */
    public DirectAllocator() {
      super(INT_BLOCK_SIZE);
    }

    @Override
    public void recycleIntBlocks(int[][] blocks, int start, int end) {
    }
  }
  
  /** array of buffers currently used in the pool. Buffers are allocated if needed don't modify this outside of this class */
  // 也就是一个有10层一维数组的二维数组，同一时刻10层中的某一层用来存储数据，并且这一层就是 head buffer(定义)
  public int[][] buffers = new int[10][];

  /** index into the buffers array pointing to the current buffer used as the head */
  // 默认buffers是一个int[10][]二维数组，也就是有10个一维数组，bufferUpto表示正在使用第几个一维数组，并且这个一维数组就是head buffer
  private int bufferUpto = -1;
  /** Pointer to the current position in head buffer */
  // 描述head buffer中被使用的位置(一维数组下标值), 此位置之前的数组空间都被使用过了
  public int intUpto = INT_BLOCK_SIZE;
  /** Current head buffer */
  // 当前正在使用的buffer，也就是head buffer
  public int[] buffer;
  /** Current head offset */
  // 在head buffer 在二维数组中的偏移
  public int intOffset = -INT_BLOCK_SIZE;

  private final Allocator allocator;

  /**
   * Creates a new {@link IntBlockPool} with a default {@link Allocator}.
   * @see IntBlockPool#nextBuffer()
   */
  public IntBlockPool() {
    this(new DirectAllocator());
  }
  
  /**
   * Creates a new {@link IntBlockPool} with the given {@link Allocator}.
   * @see IntBlockPool#nextBuffer()
   */
  public IntBlockPool(Allocator allocator) {
    this.allocator = allocator;
  }
  
  /**
   * Resets the pool to its initial state reusing the first buffer. Calling
   * {@link IntBlockPool#nextBuffer()} is not needed after reset.
   */
  public void reset() {
    this.reset(true, true);
  }
  
  /**
   * Expert: Resets the pool to its initial state reusing the first buffer. 
   * @param zeroFillBuffers if <code>true</code> the buffers are filled with <tt>0</tt>. 
   *        This should be set to <code>true</code> if this pool is used with 
   *        {@link SliceWriter}.
   * @param reuseFirst if <code>true</code> the first buffer will be reused and calling
   *        {@link IntBlockPool#nextBuffer()} is not needed after reset iff the 
   *        block pool was used before ie. {@link IntBlockPool#nextBuffer()} was called before.
   */
  public void reset(boolean zeroFillBuffers, boolean reuseFirst) {
    if (bufferUpto != -1) {
      // We allocated at least one buffer

      if (zeroFillBuffers) {
        for(int i=0;i<bufferUpto;i++) {
          // Fully zero fill buffers that we fully used
          Arrays.fill(buffers[i], 0);
        }
        // Partial zero fill the final buffer
        Arrays.fill(buffers[bufferUpto], 0, intUpto, 0);
      }
     
      if (bufferUpto > 0 || !reuseFirst) {
        final int offset = reuseFirst ? 1 : 0;  
        // Recycle all but the first buffer
        allocator.recycleIntBlocks(buffers, offset, 1+bufferUpto);
        Arrays.fill(buffers, offset, bufferUpto+1, null);
      }
      if (reuseFirst) {
        // Re-use the first buffer
        bufferUpto = 0;
        intUpto = 0;
        intOffset = 0;
        buffer = buffers[0];
      } else {
        bufferUpto = -1;
        intUpto = INT_BLOCK_SIZE;
        intOffset = -INT_BLOCK_SIZE;
        buffer = null;
      }
    }
  }
  
  /**
   * Advances the pool to its next buffer. This method should be called once
   * after the constructor to initialize the pool. In contrast to the
   * constructor a {@link IntBlockPool#reset()} call will advance the pool to
   * its first buffer immediately.
   */
  public void nextBuffer() {
    // 判断二维数组是否存储已满，那么就扩容，并且扩容结束后，迁移数据
    if (1+bufferUpto == buffers.length) {
      int[][] newBuffers = new int[(int) (buffers.length*1.5)][];
      System.arraycopy(buffers, 0, newBuffers, 0, buffers.length);
      buffers = newBuffers;
    }
    // 生成一个新的一维数组
    buffer = buffers[1+bufferUpto] = allocator.getIntBlock();
    bufferUpto++;
    // head buffer数组的可使用位置(下标值)置为0
    intUpto = 0;
    intOffset += INT_BLOCK_SIZE;
  }
  
  /**
   * Creates a new int slice with the given starting size and returns the slices offset in the pool.
   * @see SliceReader
   */
  private int newSlice(final int size) {
    if (intUpto > INT_BLOCK_SIZE-size) {
      nextBuffer();
      assert assertSliceBuffer(buffer);
    }

    final int upto = intUpto;
    // 分配size个大小的数组空间给这次的存储,然后intUpto更新
    intUpto += size;
    // 指定下次分片的级别
    buffer[intUpto-1] = 1;
    return upto;
  }
  
  private static final boolean assertSliceBuffer(int[] buffer) {
    int count = 0;
    for (int i = 0; i < buffer.length; i++) {
      count += buffer[i]; // for slices the buffer must only have 0 values
    }
    return count == 0;
  }
  
  
  // no need to make this public unless we support different sizes
  // TODO make the levels and the sizes configurable
  /**
   * An array holding the offset into the {@link IntBlockPool#LEVEL_SIZE_ARRAY}
   * to quickly navigate to the next slice level.
   */
  private final static int[] NEXT_LEVEL_ARRAY = {1, 2, 3, 4, 5, 6, 7, 8, 9, 9};
  
  /**
   * An array holding the level sizes for int slices.
   */
  private final static int[] LEVEL_SIZE_ARRAY = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};
  
  /**
   * The first level size for new slices
   */
  private final static int FIRST_LEVEL_SIZE = LEVEL_SIZE_ARRAY[0];

  /**
   * Allocates a new slice from the given offset
   */
  private int allocSlice(final int[] slice, final int sliceOffset) {
    // 取出分片的层级
    final int level = slice[sliceOffset];
    // 获得新的分片的层级
    final int newLevel = NEXT_LEVEL_ARRAY[level-1];
    // 根据新的分片的层级，获得新分片的大小
    final int newSize = LEVEL_SIZE_ARRAY[newLevel];
    // Maybe allocate another block
    if (intUpto > INT_BLOCK_SIZE-newSize) {
      nextBuffer();
      assert assertSliceBuffer(buffer);
    }

    // 获得当前在head buffer中下一个可以使用的位置
    final int newUpto = intUpto;
    // 记录下一个数组下标位置(这个位置的值在二维数组中的位置)
    final int offset = newUpto + intOffset;
    // 更新head buffer中下一个可以使用的位置
    intUpto += newSize;
    // Write forwarding address at end of last slice:
    // 将sliceOffset位置的数组值替换为offset, 目的就是在读取数据时，被告知应该跳转到数组的哪一个位置继续找这个term的其他偏移值(在文本中的偏移量)跟payload值
    // 换句话如果一篇文档的某个term出现多次，那么记录这个term的在文本中的所有偏移值跟payload并不是连续存储的
    slice[sliceOffset] = offset;
    // 记录新的分片层级
    buffer[intUpto-1] = newLevel;
    // 返回可以写入数据的位置
    return newUpto;
  }
  
  /**
   * A {@link SliceWriter} that allows to write multiple integer slices into a given {@link IntBlockPool}.
   * 
   *  @see SliceReader
   *  @lucene.internal
   */
  // 同一个域名的所有域值的信息都写在同一个二维数组中的不同的位置, 并且一个域值的所有信息并不是连续存储的。所以这里用 slices 来表示一个域值的所有信息
  public static class SliceWriter {

    // offset的值是 (head buffer这个一维数组组内的偏移量 + head buffer这个一维数组在二维数组中的偏移量)
    private int offset;
    private final IntBlockPool pool;
    
    
    public SliceWriter(IntBlockPool pool) {
      this.pool = pool;
    }
    /**
     * 
     */
    public void reset(int sliceOffset) {
      this.offset = sliceOffset;
    }
    
    /**
     * Writes the given value into the slice and resizes the slice if needed
     */
    public void writeInt(int value) {
        // 获得head buffer这个一维数组, offset >> INT_BLOCK_SHIFT的值就是head buffer在二维数组中的行数
      int[] ints = pool.buffers[offset >> INT_BLOCK_SHIFT];
      assert ints != null;
      // 获得在head buffer这个一维数组组内的偏移
      int relativeOffset = offset & INT_BLOCK_MASK;
      // if语句为真，说明分片剩余空间不足，我们需要分配新的分片(slice)
      if (ints[relativeOffset] != 0) {
        // End of slice; allocate a new one
        //  分配一个新的分片，并且返回可以存放value的下标值
          relativeOffset = pool.allocSlice(ints, relativeOffset);

        // 更新下ints变量和offset变量，因为调用pool.allocSlice()后，head buffer发生了变化
        ints = pool.buffer;
        offset = relativeOffset + pool.intOffset;
      }
      // 存储value的值
      ints[relativeOffset] = value;
      offset++; 
    }
    
    /**
     * starts a new slice and returns the start offset. The returned value
     * should be used as the start offset to initialize a {@link SliceReader}.
     */
    public int startNewSlice() {
      // offset的值是 head buffer这个一维数组组内的偏移量 + head buffer这个一维数组在二维数组中的偏移量
      return offset = pool.newSlice(FIRST_LEVEL_SIZE) + pool.intOffset;
      
    }
    
    /**
     * Returns the offset of the currently written slice. The returned value
     * should be used as the end offset to initialize a {@link SliceReader} once
     * this slice is fully written or to reset the this writer if another slice
     * needs to be written.
     */
    public int getCurrentOffset() {
      return offset;
    }
  }
  
  /**
   * A {@link SliceReader} that can read int slices written by a {@link SliceWriter}
   * @lucene.internal
   */
  public static final class SliceReader {
    
    private final IntBlockPool pool;
    // 当前读取的位置
    private int upto;
    // 指定了二维数组的某个一维数组。
    private int bufferUpto;
    // 一维数组的第一个元素在二维数组中的偏移量(位置)
    private int bufferOffset;
    // 当前正在读取数据的一维数组
    private int[] buffer;
    // limit作为下标描述了下一个存储当前term信息的位置
    private int limit;
    // 获得分片的层级
    private int level;
    // 这个term的最后一个信息的位置
    private int end;
    
    /**
     * Creates a new {@link SliceReader} on the given pool
     */
    public SliceReader(IntBlockPool pool) {
      this.pool = pool;
    }

    /**
     * Resets the reader to a slice give the slices absolute start and end offset in the pool
     */
    // 复用SliceReader对象, 重新初始化一些数据
    public void reset(int startOffset, int endOffset) {
      // 计算出在二维数组中的第几层
      bufferUpto = startOffset / INT_BLOCK_SIZE;
      // bufferUpto这一层在二维数组中的起始位置
      bufferOffset = bufferUpto * INT_BLOCK_SIZE;
      this.end = endOffset;
      upto = startOffset;
      level = 1;

      // 取出bufferUpto这一层的一维数组
      buffer = pool.buffers[bufferUpto];
      // 计算出在一维数组内的起始位置
      upto = startOffset & INT_BLOCK_MASK;

      final int firstSize = IntBlockPool.LEVEL_SIZE_ARRAY[0];
      if (startOffset+firstSize >= endOffset) {
        // There is only this one slice to read
        // 说明term的所有信息都在 bufferUpto这一层的一维数组中
        limit = endOffset & INT_BLOCK_MASK;
      } else {
        limit = upto+firstSize-1;
      }

    }
    
    /**
     * Returns <code>true</code> iff the current slice is fully read. If this
     * method returns <code>true</code> {@link SliceReader#readInt()} should not
     * be called again on this slice.
     */
    public boolean endOfSlice() {
      assert upto + bufferOffset <= end;
      return upto + bufferOffset == end;
    }
    
    /**
     * Reads the next int from the current slice and returns it.
     * @see SliceReader#endOfSlice()
     */
    // 读取下一个head buffer中的int值
    public int readInt() {
      assert !endOfSlice();
      assert upto <= limit;
      if (upto == limit)
        nextSlice();
      return buffer[upto++];
    }
    
    private void nextSlice() {
      // Skip to our next slice
      // 找到下一个存储term信息的位置
      final int nextIndex = buffer[limit];
      // 获得分片的层级
      level = NEXT_LEVEL_ARRAY[level-1];
      // 获得分片的大小
      final int newSize = LEVEL_SIZE_ARRAY[level];
      // 计算出在二维数组中的第几层
      bufferUpto = nextIndex / INT_BLOCK_SIZE;
      // 当前的一维数组的第一个元素在二维数组中的位置
      bufferOffset = bufferUpto * INT_BLOCK_SIZE;
      // 取出 head buffer
      buffer = pool.buffers[bufferUpto];
      // 计算出在head buffer中的起始位置
      upto = nextIndex & INT_BLOCK_MASK;

      // 更新limit的值
      if (nextIndex + newSize >= end) {
        // We are advancing to the final slice
        // 已经读到最后一个slice
        assert end - nextIndex > 0;
        limit = end - bufferOffset;
      } else {
        // This is not the final slice (subtract 4 for the
        // forwarding address at the end of this new slice)
        // 还有其他的slice没有读取到, 将limit的值置为下一个slice的起始位置
        limit = upto+newSize-1;
      }
    }
  }
}

