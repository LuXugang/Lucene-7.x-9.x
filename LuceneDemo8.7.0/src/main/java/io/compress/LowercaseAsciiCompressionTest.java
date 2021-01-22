package io.compress;

import org.apache.lucene.store.ByteBuffersDataOutput;
import org.apache.lucene.util.compress.LowercaseAsciiCompression;

import java.io.IOException;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2021/1/22 11:31 上午
 */
public class LowercaseAsciiCompressionTest {
    public static void main(String[] args) throws IOException {
        ByteBuffersDataOutput out = new ByteBuffersDataOutput();
        int length = 100;
        byte[] input = getRandomCharacters(length);
        byte[] tmp = new byte[length];
        boolean isCompressed = LowercaseAsciiCompression.compress(input, length, tmp, out);
        System.out.println("isCompressed: "+isCompressed+"" + " source length: "+length+"" + " temp length: "+tmp.length+"");
    }

    private static byte[] getRandomCharacters(int length){
        String lowercaseSet = "qwertyuiopasdfghjklzxcvbnm";
        Random random = new Random();
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            char c =  lowercaseSet.charAt(random.nextInt(lowercaseSet.length()));
            result[i] = (byte)c;
         }
        return result;
    }
}
