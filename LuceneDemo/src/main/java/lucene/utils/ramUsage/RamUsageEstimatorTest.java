package lucene.utils.ramUsage;

import org.apache.lucene.util.RamUsageEstimator;

/**
 * @author Lu Xugang
 * @date 2019/12/10 4:46 下午
 */
public class RamUsageEstimatorTest {

    private static int aa = 0;
    private static int bb = 0;
    private static int cc = 0;
    private int a;
    private int b;
    private int c;
    private static int d;
    private Object1 object1;

    public RamUsageEstimatorTest(int a, int b, int c, Object1 object1) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.object1 = object1;
    }

    public static void main(String[] args) throws Throwable{
        RamUsageEstimatorTest ramUsageEstimator = new RamUsageEstimatorTest(1, 2, 3, new Object1());
        RamUsageEstimator.shallowSizeOf(ramUsageEstimator);

        int sum = ramUsageEstimator.a + ramUsageEstimator.b + ramUsageEstimator.c;
        Thread.sleep(300000000);
    }
}
