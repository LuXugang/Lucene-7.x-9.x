package org.example;

import java.nio.ByteBuffer;

public class TestCompressBytes {
    public static void main(String[] args) {
        int dimension = 6;
        byte[] raw = new byte[dimension];
        raw[0] = 5;
        raw[1] = 8;
        raw[2] = 10;
        raw[3] = 11;
        raw[4] = 12;
        raw[5] = 13;
        byte[] compressed = new byte[(dimension + 1) >> 1];;
        for (int i = 0; i < compressed.length; ++i) {
            int v = (raw[i] << 4) | raw[compressed.length + i];
            compressed[i] = (byte) v;
        }
        System.out.printf("abc");
    }
}
