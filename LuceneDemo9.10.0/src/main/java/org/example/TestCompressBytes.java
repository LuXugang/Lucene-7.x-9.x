package org.example;

import java.nio.ByteBuffer;

public class TestCompressBytes {
    public static void main(String[] args) {
        int dimension = 3;
        byte[] raw = new byte[dimension];
        raw[0] = 5;
        raw[1] = 8;
        raw[2] = 10;
        byte[] compressed = new byte[(dimension + 1) >> 1];;
        for (int i = 0; i < compressed.length; ++i) {
            int v = (raw[i] << 4) | raw[compressed.length + i];
            compressed[i] = (byte) v;
        }
        System.out.printf("abc");
    }
}
