package lucene.compress.dedupAndEncodeTest;

import org.apache.lucene.util.BytesRef;

import java.util.Arrays;

/**
 * @author Lu Xugang
 * @date 2019-01-30 17:38
 */
public class ConvertIntToByteRef {

  public static BytesRef dedupAndEncode(int[] ordinals) {
    // 对 ordinal排序，为了下面存储差值
    Arrays.sort(ordinals, 0, ordinals.length);
    // 先给每一个int类型分配5个字节大小的空间, 每个字节中只有7位是有效字节(描述数值),最高位是个定界符, 所以一个int类型最多要5个字节
    byte[] bytes = new byte[5*ordinals.length];
    int lastOrd = -1;
    int upto = 0;
    // 遍历处理每一个int类型
    for(int i=0;i<ordinals.length;i++) {
      int ord = ordinals[i];
      // ord could be == lastOrd, so we must dedup:
      // 去重操作
      if (ord > lastOrd) {
        int delta;
        if (lastOrd == -1) {
          // 处理第一个值, 只能储存原始的数值
          delta = ord;
        } else {
          // 处理非第一个值，就可以储存这个值与前一个值的差值
          delta = ord - lastOrd;
        }
        // 当前数值在0~127范围内
        if ((delta & ~0x7F) == 0) {
          // 注意的是第8位是0(位数从1开始), 是个定界符, 表示下一个byte字节是另一个int的一部分
          bytes[upto] = (byte) delta;
          upto++;
        } else if ((delta & ~0x3FFF) == 0) {
          // 这个字节的最高位是1, 表示下一个byte字节和当前字节属于同一个int类型的一部分
          bytes[upto] = (byte) (0x80 | ((delta & 0x3F80) >> 7));
          // 每次存储7位
          bytes[upto + 1] = (byte) (delta & 0x7F);
          upto += 2;
        } else if ((delta & ~0x1FFFFF) == 0) {
          bytes[upto] = (byte) (0x80 | ((delta & 0x1FC000) >> 14));
          bytes[upto + 1] = (byte) (0x80 | ((delta & 0x3F80) >> 7));
          bytes[upto + 2] = (byte) (delta & 0x7F);
          upto += 3;
        } else if ((delta & ~0xFFFFFFF) == 0) {
          bytes[upto] = (byte) (0x80 | ((delta & 0xFE00000) >> 21));
          bytes[upto + 1] = (byte) (0x80 | ((delta & 0x1FC000) >> 14));
          bytes[upto + 2] = (byte) (0x80 | ((delta & 0x3F80) >> 7));
          bytes[upto + 3] = (byte) (delta & 0x7F);
          upto += 4;
        } else {
          bytes[upto] = (byte) (0x80 | ((delta & 0xF0000000) >> 28));
          bytes[upto + 1] = (byte) (0x80 | ((delta & 0xFE00000) >> 21));
          bytes[upto + 2] = (byte) (0x80 | ((delta & 0x1FC000) >> 14));
          bytes[upto + 3] = (byte) (0x80 | ((delta & 0x3F80) >> 7));
          bytes[upto + 4] = (byte) (delta & 0x7F);
          upto += 5;
        }
        // 这里将ord保存下来是为了去重
        lastOrd = ord;
      }
    }
    return new BytesRef(bytes, 0, upto);
  }

  public static void main(String[] args) {
    int[] array = {3, 2, 2, 8, 12};
    BytesRef ref = ConvertIntToByteRef.dedupAndEncode(array);
    System.out.println(ref.toString());
  }
}
