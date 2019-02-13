package lucene.compress.BulkOperationPackedTest;


/**
 * @author Lu Xugang
 * @date 2019-02-12 16:40
 */
public class Encode {
  public static byte[] encode(long[] values,int bitsPerValue) {
    byte[] blocks = new byte[2];
    int blocksOffset = 0;
    int nextBlock = 0;
    int bitsLeft = 8;
    int valuesOffset = 0;
    for (int i = 0; i < values.length; ++i) {
      final long v = values[valuesOffset++];
      // bitsPerValue指的是每一个元素需要占用的bit位数，这个值取决于数据中最大元素需要的bit位
      // 也就是每一个元素不管大小，占用的bit位数都是一致的
      if (bitsPerValue < bitsLeft) {
        // just buffer
        nextBlock |= v << (bitsLeft - bitsPerValue);
        bitsLeft -= bitsPerValue;
      } else {
        // flush as many blocks as possible
        int bits = bitsPerValue - bitsLeft;
        // nextBlock | (v >>> bits)的操作将v值存储到nextBlock中
        // 然后将nextBlock的值存储到blocks[]数组中，完成一个字节的压缩
        blocks[blocksOffset++] = (byte) (nextBlock | (v >>> bits));
        while (bits >= 8) {
          bits -= 8;
          // 将一个数组元素分成多块(按照一个8个bit大小划分)存储
          blocks[blocksOffset++] = (byte) (v >>> bits);
        }
        // then buffer
        bitsLeft = 8 - bits;
        // 把v的值的剩余部分存放到下一个nextBlock中,也就是当前的v值的部分值会跟下一个v值的数据(可能是部分数据)混合存储到同一个字节中
        nextBlock = (int) ((v & ((1L << bits) - 1)) << bitsLeft);
      }
    }
    return blocks;
  }


  public static void main(String[] args) {
    long[] array = {1, 1, 1, 0, 2, 2, 0, 0};
    byte[] result = Encode.encode(array, 2);
    for (int a: result
    ) {
      System.out.println(a);
    }
  }
}
