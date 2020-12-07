package io.compress;

import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.util.compress.LZ4;

import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/11/5 8:32 PM
 */
public class LZ4FastTest {

//    public static void main(String[] args) throws Exception{
//        byte[] array = new byte[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5,    // 下标值 0 ~ 9
//                1, 2, 3, 4, 5, 6, 7, 8, 9,10,    // 下标值 10 ~ 19
//                11,12,13,14,15,16,17,18,19,20,    // 下标值 20 ~ 29
//                21,22,23,24,25,26,27,28,29,30,    // 下标值 30 ~ 39
//                31,32, 2, 3, 4, 5, 6, 7, 8, 9,    // 下标值 40 ~ 49
//                10,11,12,13,14,15,16,17,18,19,    // 下标值 50 ~ 59
//                20,21,22,23,24,25,26,27,28,29,    // 下标值 60 ~ 69
//                30, 31, 32};
//        int dictOff = 0;
//        int dictLen = 0;
//        int len = array.length - dictLen;
//        ByteBuffersDataOutput out = new ByteBuffersDataOutput();
//        LZ4.compressWithDictionary(array, dictOff, dictLen, len, out, new LZ4.FastCompressionHashTable());
//        byte[] compressed = out.toArrayCopy();
//
//        Random random = new Random();
//
//        int restoreOffset = random.nextInt(10);
//        byte[] restored = new byte[restoreOffset + dictLen + len+ random.nextInt(10)];
//        System.arraycopy(array, dictOff, restored, restoreOffset, dictLen);
//        LZ4.decompress(new ByteArrayDataInput(compressed), len, restored, dictLen + restoreOffset);
//        System.out.printf("abc");
//    }
}
