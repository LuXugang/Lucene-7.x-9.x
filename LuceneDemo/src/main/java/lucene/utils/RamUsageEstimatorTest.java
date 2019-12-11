package lucene.utils;

import org.apache.lucene.util.RamUsageEstimator;
import sun.instrument.InstrumentationImpl;

import java.lang.instrument.Instrumentation;

/**
 * @author Lu Xugang
 * @date 2019/12/10 10:26 下午
 */
public class RamUsageEstimatorTest {
    private static String abc = "a";

    public static void main(String[] args) throws Exception{
        String a= "abc";
        RamUsageEstimator.shallowSizeOf(a);
        int count = 0;
        while (count++ < 99999){
            Thread.sleep(2000);
            System.out.println("hha");
        }
    }
}
