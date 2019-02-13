package lucene.compress.dedupAndEncodeTest;

import org.apache.lucene.util.BytesRef;

/**
 * @author Lu Xugang
 * @date 2019-01-30 20:04
 */
public class ConvertByteRefToInt {

    public static void decode(BytesRef bytesRef){
        byte[] bytes = bytesRef.bytes;
        int end = bytesRef.offset + bytesRef.length;
        int ord = 0;
        int offset = bytesRef.offset;
        int prev = 0;
        while (offset < end) {
            byte b = bytes[offset++];
            // if语句为真：decode结束，用ord表示
            if (b >= 0) {
                // ord的值为差值，所以(真实值 = 差值 + 前面一个值)
                prev = ord = ((ord << 7) | b) + prev;
                System.out.println(ord);
                ord = 0;
                // decode没有结束，需要继续拼接
            } else {
                ord = (ord << 7) | (b & 0x7F);
            }
        }
    }

    public static void main(String[] args) {
        int[] array = {3, 2, 2, 8, 12};
        BytesRef ref = ConvertIntToByteRef.dedupAndEncode(array);
        ConvertByteRefToInt.decode(ref);
    }
}
