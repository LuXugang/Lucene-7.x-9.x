package io.compress;

import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.util.compress.LZ4;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/11/5 8:32 PM
 */
public class LZ4FastTest {

    public static void main(String[] args) throws Exception{
        byte[] array = new byte[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5,
                1, 2, 3, 4, 5, 6, 7, 8, 9,10,
                11,12,13,14,15,16,17,18,19,20,
                21,22,23,24,25,26,27,28,29,30,
                31,32, 2, 3, 4, 5, 6, 7, 8, 9,
                10,11,12,13,14,15,16,17,18,19,
                20,21,22,23,24,25,26,27,28,29,
                30, 31, 32};
        System.out.println("before compress length: "+array.length+"");
        int dictOff = 0;
        int dictLen = 0;
        int len = array.length - dictLen;
        ByteBuffersDataOutput out = new ByteBuffersDataOutput();
        LZ4.compressWithDictionary(array, dictOff, dictLen, len, out, new LZ4.HighCompressionHashTable());
        byte[] compressed = out.toArrayCopy();
        System.out.println("after compress length: "+compressed.length+"");

        Random random = new Random();

        int restoreOffset = random.nextInt(10);
        byte[] restored = new byte[restoreOffset + dictLen + len+ random.nextInt(10)];
        System.arraycopy(array, dictOff, restored, restoreOffset, dictLen);
        LZ4.decompress(new ByteArrayDataInput(compressed), len, restored, dictLen + restoreOffset);
        for (byte b : restored) {
            System.out.printf(b + " " );
        }

    }
}
