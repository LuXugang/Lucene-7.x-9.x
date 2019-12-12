package lucene.utils.ramUsage;

import org.apache.lucene.util.RamUsageEstimator;

/**
 * @author Lu Xugang
 * @date 2019/12/12 2:10 下午
 */
public class GrandSonClass extends SonClass{
    private static long staticGrandSonData;
    int grandSonData;

    public static void main(String[] args) throws Exception{
        GrandSonClass grandSon = new GrandSonClass();
        RamUsageEstimator.shallowSizeOf(grandSon);
        Thread.sleep(300000000);
    }
}
