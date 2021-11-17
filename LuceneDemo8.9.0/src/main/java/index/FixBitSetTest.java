package index;

import org.apache.lucene.util.FixedBitSet;

public class FixBitSetTest {
    public static void main(String[] args) {
        int maxDoc = 3;
        int docId = 10;
        int minDoc = 0;
        FixedBitSet fixedBitSet = new FixedBitSet(257);
        fixedBitSet.set(minDoc, maxDoc + 1);
        fixedBitSet.set(docId);
        System.out.println("abc");
    }
}
