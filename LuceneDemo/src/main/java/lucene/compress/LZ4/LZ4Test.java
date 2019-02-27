package lucene.compress.LZ4;

import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.codecs.compressing.Decompressor;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.util.BytesRef;

/**
 * @author Lu Xugang
 * @date 2019-02-25 19:56
 */
public class LZ4Test {

  public static void main(String[] args) throws Exception{
    byte[] encodeResult = new byte[1024];
    byte[] array = {1, 2, 3, 4, 5, 1, 2, 3, 4, 5,    // 下标值 0 ~ 9
                    1, 2, 3, 4, 5, 6, 7, 8, 9,10,    // 下标值 10 ~ 19
                   11,12,13,14,15,16,17,18,19,20,    // 下标值 20 ~ 29
                   21,22,23,24,25,26,27,28,29,30,    // 下标值 30 ~ 39
                   31,32, 2, 3, 4, 5, 6, 7, 8, 9,    // 下标值 40 ~ 49
                   10,11,12,13,14,15,16,17,18,19,    // 下标值 50 ~ 59
                   20,21,22,23,24,25,26,27,28,29,    // 下标值 60 ~ 69
                   30, 31, 32};                      // 下标值 70 ~ 72
    BytesRef ref = new BytesRef();
    ByteArrayDataOutput output = new ByteArrayDataOutput(encodeResult);
    ByteArrayDataInput input = new ByteArrayDataInput(encodeResult);
    CompressionMode compressionMode = CompressionMode.FAST;
    Compressor compressor  = compressionMode.newCompressor();
    compressor.compress(array, 0, array.length, output);
    System.out.println(encodeResult);

    Decompressor decompressor = CompressionMode.FAST.newDecompressor();
    decompressor.decompress(input, array.length, 0, output.getPosition(), ref);
    System.out.println(ref);
  }
}
