import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.util.NumericUtils;

public class TestDoubleCalculation {

    public static void main(String[] args) throws Exception {

//        int aInt = 4;
//        int maxInt = Integer.MAX_VALUE;
//        int minInt = Integer.MIN_VALUE;
//        byte[] aByte = new byte[Integer.BYTES];
//        byte[] deltaByte  = new byte[Integer.BYTES];
//        deltaByte[deltaByte.length -1] = 1;
//        IntPoint.encodeDimension(aInt, aByte, 0);
//        byte[] addResult = new byte[Integer.BYTES];
//        byte[] subtractResult  = new byte[Integer.BYTES];
//        NumericUtils.subtract(4, 0, aByte, deltaByte, subtractResult);
//        NumericUtils.add(4, 0, aByte, deltaByte, addResult);
//        byte[] maxIntByte = new byte[Integer.BYTES];
//        byte[] minIntByte = new byte[Integer.BYTES];
//        IntPoint.encodeDimension(maxInt, maxIntByte, 0);
//        IntPoint.encodeDimension(minInt, minIntByte, 0);
//        System.out.println("int greater result: "+IntPoint.decodeDimension(addResult, 0)+"");
//        System.out.println("int less result: "+IntPoint.decodeDimension(subtractResult, 0)+"");
//        System.out.println("------------------------");
//
//        long aLong = 4;
//        long maxLong = Long.MAX_VALUE;
//        long minLong = Long.MIN_VALUE;
//        aByte = new byte[Long.BYTES];
//        deltaByte  = new byte[Long.BYTES];
//        deltaByte[deltaByte.length -1] = 1;
//        LongPoint.encodeDimension(aLong, aByte, 0);
//        addResult = new byte[Long.BYTES];
//        subtractResult  = new byte[Long.BYTES];
//        NumericUtils.subtract(8, 0, aByte, deltaByte, subtractResult);
//        NumericUtils.add(8, 0, aByte, deltaByte, addResult);
//        byte[] maxLongByte = new byte[Long.BYTES];
//        byte[] minLongByte = new byte[Long.BYTES];
//        LongPoint.encodeDimension(maxLong, maxLongByte, 0);
//        LongPoint.encodeDimension(minLong, minLongByte, 0);
//        System.out.println("long greater result: "+LongPoint.decodeDimension(addResult, 0)+"");
//        System.out.println("long less result: "+LongPoint.decodeDimension(subtractResult, 0)+"");
//        System.out.println("------------------------");
//
//
//        float aFloat = 4f;
//        float maxFloat = Float.MAX_VALUE;
//        float minFloat = Float.MIN_VALUE;
//        aByte = new byte[Float.BYTES];
//        deltaByte  = new byte[Float.BYTES];
//        deltaByte[deltaByte.length -1] = 1;
//        FloatPoint.encodeDimension(aFloat, aByte, 0);
//        addResult = new byte[Float.BYTES];
//        subtractResult  = new byte[Float.BYTES];
//        NumericUtils.subtract(4, 0, aByte, deltaByte, subtractResult);
//        NumericUtils.add(4, 0, aByte, deltaByte, addResult);
//        byte[] maxFloatByte = new byte[Float.BYTES];
//        byte[] minFloatByte = new byte[Float.BYTES];
//        FloatPoint.encodeDimension(maxFloat, maxFloatByte, 0);
//        FloatPoint.encodeDimension(minFloat, minFloatByte, 0);
//        System.out.println("float greater result: "+FloatPoint.decodeDimension(addResult, 0)+"");
//        System.out.println("float less result: "+FloatPoint.decodeDimension(subtractResult, 0)+"");
//        System.out.println("------------------------");
//
//        double aDouble = 4d;
//        double maxDouble = Double.MAX_VALUE;
//        double minDouble = Double.MIN_VALUE;
//        aByte = new byte[Double.BYTES];
//        deltaByte  = new byte[Double.BYTES];
//        deltaByte[deltaByte.length -1] = 1;
//        DoublePoint.encodeDimension(aDouble, aByte, 0);
//        addResult = new byte[Double.BYTES];
//        subtractResult  = new byte[Double.BYTES];
//        NumericUtils.subtract(8, 0, aByte, deltaByte, subtractResult);
//        NumericUtils.add(8, 0, aByte, deltaByte, addResult);
//        byte[] maxDoubleByte = new byte[Double.BYTES];
//        byte[] minDoubleByte = new byte[Double.BYTES];
//        DoublePoint.encodeDimension(maxDouble, maxDoubleByte, 0);
//        DoublePoint.encodeDimension(minDouble, minDoubleByte, 0);
//        System.out.println("double greater result: "+DoublePoint.decodeDimension(addResult, 0)+"");
//        System.out.println("double less result: "+DoublePoint.decodeDimension(subtractResult, 0)+"");
//        System.out.println("------------------------");

        int maxInt = Integer.MAX_VALUE;
        int minInt = Integer.MIN_VALUE;
        byte[] maxIntByte = new byte[Integer.BYTES];
        byte[] minIntByte = new byte[Integer.BYTES];
        IntPoint.encodeDimension(maxInt, maxIntByte, 0);
        IntPoint.encodeDimension(minInt, minIntByte, 0);
        System.out.println("------------------------");

        float maxFloat = Float.MAX_VALUE;
        float minFloat = Float.MIN_VALUE;
        byte[] maxFloatByte = new byte[Float.BYTES];
        byte[] minFloatByte = new byte[Float.BYTES];
        FloatPoint.encodeDimension(maxFloat, maxFloatByte, 0);
        FloatPoint.encodeDimension(minFloat, minFloatByte, 0);

        float floatA = FloatPoint.decodeDimension(maxFloatByte, 0);
        float floatB = floatA + 0.1f;

        int a = 100;


        System.out.println("abc");
    }
}
