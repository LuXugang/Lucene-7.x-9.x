import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.NumericUtils;

import java.util.Random;

public class TestNumericUtil {
    public static void main(String[] args) {

        long aLong = 2;
        long bLong = -2;
        byte[] aByte = new byte[Long.BYTES];
        byte[] bByte = new byte[Long.BYTES];

        NumericUtils.longToSortableBytes(aLong, aByte, 0);
        NumericUtils.longToSortableBytes(bLong, bByte, 0);


//        long a = Double.doubleToLongBits(-1.234);
//        long b = Double.doubleToLongBits(-2.345);
//        System.out.println("a: "+a);
//        System.out.println("b: "+b);
//        if(a > b){
//            System.out.println("a > b");
//        }else if(a < b) {
//            System.out.println("a < b");
//        }
        NumericUtils.doubleToSortableLong(-1.234);
        NumericUtils.doubleToSortableLong(1.234);

        long a = Double.doubleToLongBits(1.234);
        long b = Double.doubleToLongBits(2.345);
        System.out.println("a: "+a);
        System.out.println("b: "+b);
        if(a > b){
            System.out.println("a > b");
        }else if(a < b) {
            System.out.println("a < b");
        }
    }
}
