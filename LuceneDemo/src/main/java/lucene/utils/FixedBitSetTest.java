package lucene.utils;

import org.apache.lucene.util.FixedBitSet;

/**
 * @author Lu Xugang
 * @date 2019-04-03 22:28
 */
public class FixedBitSetTest {

    public static void main(String[] args) {

        FixedBitSet fixedBitSet = new FixedBitSet(300);
        fixedBitSet.set(3);
        fixedBitSet.set(67);
        fixedBitSet.set(120);
        fixedBitSet.set(179);
        fixedBitSet.set(195);
        fixedBitSet.set(313);



        boolean b = fixedBitSet.get(65);
        System.out.println(""+b+"");
//        int a = fixedBitSet.nextSetBit(299);
        System.out.println(a);
    }

}
