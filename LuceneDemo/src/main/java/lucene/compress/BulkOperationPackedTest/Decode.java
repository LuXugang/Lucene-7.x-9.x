package lucene.compress.BulkOperationPackedTest;

/**
 * @author Lu Xugang
 * @date 2019-02-13 17:06
 */
public class Decode {

  public static void decode(byte[] blocks, int blocksOffset, long[] values, int valuesOffset, int iterations) {
    for (int j = 0; j < iterations; ++j) {
      final byte block = blocks[blocksOffset++];
      values[valuesOffset++] = (block >>> 6) & 3;
      values[valuesOffset++] = (block >>> 4) & 3;
      values[valuesOffset++] = (block >>> 2) & 3;
      values[valuesOffset++] = block & 3;
    }
  }

  public static void main(String[] args) {
    long[] array = {1, 1, 1, 0, 2, 2, 0, 0};
    byte[] result = Encode.encode(array, 2);
    System.out.println("Encode");
    for (long a: result
    ) {
      System.out.println(a);
    }
    long [] arrayDecode = new long[8];
    Decode.decode(result, 0, arrayDecode, 0, 2);
    System.out.println("Decode");
    for (long a : arrayDecode
         ) {
      System.out.println(a);
    }
  }
}
