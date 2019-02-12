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
package org.apache.lucene.util.packed;



/**
 * Non-specialized {@link BulkOperation} for {@link PackedInts.Format#PACKED}.
 */
class BulkOperationPacked extends BulkOperation {

  private final int bitsPerValue;
  private final int longBlockCount;
  private final int longValueCount;
  private final int byteBlockCount;
  private final int byteValueCount;
  private final long mask;
  private final int intMask;

  public BulkOperationPacked(int bitsPerValue) {
    this.bitsPerValue = bitsPerValue;
    assert bitsPerValue > 0 && bitsPerValue <= 64;
    int blocks = bitsPerValue;
    while ((blocks & 1) == 0) {
      blocks >>>= 1;
    }
    this.longBlockCount = blocks;
    this.longValueCount = 64 * longBlockCount / bitsPerValue;
    int byteBlockCount = 8 * longBlockCount;
    int byteValueCount = longValueCount;
    while ((byteBlockCount & 1) == 0 && (byteValueCount & 1) == 0) {
      byteBlockCount >>>= 1;
      byteValueCount >>>= 1;
    }
    this.byteBlockCount = byteBlockCount;
    this.byteValueCount = byteValueCount;
    if (bitsPerValue == 64) {
      this.mask = ~0L;
    } else {
      this.mask = (1L << bitsPerValue) - 1;
    }
    this.intMask = (int) mask;
    assert longValueCount * bitsPerValue == 64 * longBlockCount;
  }

  @Override
  public int longBlockCount() {
    return longBlockCount;
  }

  @Override
  public int longValueCount() {
    return longValueCount;
  }

  @Override
  public int byteBlockCount() {
    return byteBlockCount;
  }

  @Override
  public int byteValueCount() {
    return byteValueCount;
  }

  @Override
  public void decode(long[] blocks, int blocksOffset, long[] values,
      int valuesOffset, int iterations) {
    int bitsLeft = 64;
    for (int i = 0; i < longValueCount * iterations; ++i) {
      bitsLeft -= bitsPerValue;
      if (bitsLeft < 0) {
        values[valuesOffset++] =
            ((blocks[blocksOffset++] & ((1L << (bitsPerValue + bitsLeft)) - 1)) << -bitsLeft)
            | (blocks[blocksOffset] >>> (64 + bitsLeft));
        bitsLeft += 64;
      } else {
        values[valuesOffset++] = (blocks[blocksOffset] >>> bitsLeft) & mask;
      }
    }
  }

  @Override
  public void decode(byte[] blocks, int blocksOffset, long[] values,
      int valuesOffset, int iterations) {
    long nextValue = 0L;
    int bitsLeft = bitsPerValue;
    for (int i = 0; i < iterations * byteBlockCount; ++i) {
      final long bytes = blocks[blocksOffset++] & 0xFFL;
      if (bitsLeft > 8) {
        // just buffer
        bitsLeft -= 8;
        nextValue |= bytes << bitsLeft;
      } else {
        // flush
        int bits = 8 - bitsLeft;
        values[valuesOffset++] = nextValue | (bytes >>> bits);
        while (bits >= bitsPerValue) {
          bits -= bitsPerValue;
          values[valuesOffset++] = (bytes >>> bits) & mask;
        }
        // then buffer
        bitsLeft = bitsPerValue - bits;
        nextValue = (bytes & ((1L << bits) - 1)) << bitsLeft;
      }
    }
    assert bitsLeft == bitsPerValue;
  }

  @Override
  public void decode(long[] blocks, int blocksOffset, int[] values,
      int valuesOffset, int iterations) {
    if (bitsPerValue > 32) {
      throw new UnsupportedOperationException("Cannot decode " + bitsPerValue + "-bits values into an int[]");
    }
    int bitsLeft = 64;
    for (int i = 0; i < longValueCount * iterations; ++i) {
      bitsLeft -= bitsPerValue;
      if (bitsLeft < 0) {
        values[valuesOffset++] = (int)
            (((blocks[blocksOffset++] & ((1L << (bitsPerValue + bitsLeft)) - 1)) << -bitsLeft)
            | (blocks[blocksOffset] >>> (64 + bitsLeft)));
        bitsLeft += 64;
      } else {
        values[valuesOffset++] = (int) ((blocks[blocksOffset] >>> bitsLeft) & mask);
      }
    }
  }

  @Override
  public void decode(byte[] blocks, int blocksOffset, int[] values,
      int valuesOffset, int iterations) {
    int nextValue = 0;
    int bitsLeft = bitsPerValue;
    for (int i = 0; i < iterations * byteBlockCount; ++i) {
      final int bytes = blocks[blocksOffset++] & 0xFF;
      if (bitsLeft > 8) {
        // just buffer
        bitsLeft -= 8;
        nextValue |= bytes << bitsLeft;
      } else {
        // flush
        int bits = 8 - bitsLeft;
        values[valuesOffset++] = nextValue | (bytes >>> bits);
        while (bits >= bitsPerValue) {
          bits -= bitsPerValue;
          values[valuesOffset++] = (bytes >>> bits) & intMask;
        }
        // then buffer
        bitsLeft = bitsPerValue - bits;
        nextValue = (bytes & ((1 << bits) - 1)) << bitsLeft;
      }
    }
    assert bitsLeft == bitsPerValue;
  }

  @Override
  public void encode(long[] values, int valuesOffset, long[] blocks,
      int blocksOffset, int iterations) {
    long nextBlock = 0;
    int bitsLeft = 64;
    for (int i = 0; i < longValueCount * iterations; ++i) {
      bitsLeft -= bitsPerValue;
      if (bitsLeft > 0) {
        nextBlock |= values[valuesOffset++] << bitsLeft;
      } else if (bitsLeft == 0) {
        nextBlock |= values[valuesOffset++];
        blocks[blocksOffset++] = nextBlock;
        nextBlock = 0;
        bitsLeft = 64;
      } else { // bitsLeft < 0
        nextBlock |= values[valuesOffset] >>> -bitsLeft;
        blocks[blocksOffset++] = nextBlock;
        nextBlock = (values[valuesOffset++] & ((1L << -bitsLeft) - 1)) << (64 + bitsLeft);
        bitsLeft += 64;
      }
    }
  }

  @Override
  public void encode(int[] values, int valuesOffset, long[] blocks,
      int blocksOffset, int iterations) {
    long nextBlock = 0;
    int bitsLeft = 64;
    for (int i = 0; i < longValueCount * iterations; ++i) {
      bitsLeft -= bitsPerValue;
      if (bitsLeft > 0) {
        nextBlock |= (values[valuesOffset++] & 0xFFFFFFFFL) << bitsLeft;
      } else if (bitsLeft == 0) {
        nextBlock |= (values[valuesOffset++] & 0xFFFFFFFFL);
        blocks[blocksOffset++] = nextBlock;
        nextBlock = 0;
        bitsLeft = 64;
      } else { // bitsLeft < 0
        nextBlock |= (values[valuesOffset] & 0xFFFFFFFFL) >>> -bitsLeft;
        blocks[blocksOffset++] = nextBlock;
        nextBlock = (values[valuesOffset++] & ((1L << -bitsLeft) - 1)) << (64 + bitsLeft);
        bitsLeft += 64;
      }
    }
  }

  @Override
  public void encode(long[] values, int valuesOffset, byte[] blocks,
      int blocksOffset, int iterations) {
    int nextBlock = 0;
    int bitsLeft = 8;
    for (int i = 0; i < byteValueCount * iterations; ++i) {
      final long v = values[valuesOffset++];
      assert PackedInts.unsignedBitsRequired(v) <= bitsPerValue;
      // bitsPerValue指的是每一个元素需要占用的bit位数，这个值在初始化DirectWriter时候定义的
      // 也就是每一个元素不管大小，占用的bit位数都是一致的
      if (bitsPerValue < bitsLeft) {
        // just buffer
        // 这个操作说明从最高位开始存储, 每次存储占用nextBlock的bitsPerValue个bit位
        nextBlock |= v << (bitsLeft - bitsPerValue);
        bitsLeft -= bitsPerValue;
      } else {
        // flush as many blocks as possible
        int bits = bitsPerValue - bitsLeft;
        // nextBlock | (v >>> bits)的操作将v值存储到nextBlock中
        // 然后将nextBlock的值存储到blocks[]数组中，完成一个字节的压缩
        blocks[blocksOffset++] = (byte) (nextBlock | (v >>> bits));
        // 每个元素需要的bit位的个数大于8时的处理
        while (bits >= 8) {
          bits -= 8;
          // 将一个元素分成多块(按照一个8个bit大小划分)存储
          blocks[blocksOffset++] = (byte) (v >>> bits);
        }
        // then buffer
        bitsLeft = 8 - bits;
        // 把v的值的剩余部分存放到下一个nextBlock中,也就是当前的v值的部分值会跟下一个v值的数据(可能是部分数据)混合存储到同一个字节中
        nextBlock = (int) ((v & ((1L << bits) - 1)) << bitsLeft);
      }
    }
    assert bitsLeft == 8;
  }

  @Override
  public void encode(int[] values, int valuesOffset, byte[] blocks,
      int blocksOffset, int iterations) {
    int nextBlock = 0;
    int bitsLeft = 8;
    for (int i = 0; i < byteValueCount * iterations; ++i) {
      final int v = values[valuesOffset++];
      assert PackedInts.bitsRequired(v & 0xFFFFFFFFL) <= bitsPerValue;
      if (bitsPerValue < bitsLeft) {
        // just buffer
        nextBlock |= v << (bitsLeft - bitsPerValue);
        bitsLeft -= bitsPerValue;
      } else {
        // flush as many blocks as possible
        int bits = bitsPerValue - bitsLeft;
        blocks[blocksOffset++] = (byte) (nextBlock | (v >>> bits));
        while (bits >= 8) {
          bits -= 8;
          blocks[blocksOffset++] = (byte) (v >>> bits);
        }
        // then buffer
        bitsLeft = 8 - bits;
        nextBlock = (v & ((1 << bits) - 1)) << bitsLeft;
      }
    }
    assert bitsLeft == 8;
  }

}
