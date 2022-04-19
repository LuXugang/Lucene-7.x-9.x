import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.FixedBitSet;

public class FixBitSetVsDISIAll {
    public static void main(String[] args) throws Exception{
        int[] maxDocs = {100000, 1000000, 5000000, 10000000, 20000000, 50000000};
        for (int maxDoc: maxDocs) {

        FixedBitSet fixedBitSet = new FixedBitSet(maxDoc);
        fixedBitSet.set(0, maxDoc);


        DocIdSetIterator disiAll= DocIdSetIterator.all(maxDoc);
        BitDocIdSet bitDocIdSet = new BitDocIdSet(fixedBitSet, maxDoc);
        DocIdSetIterator disiFixBitSet= bitDocIdSet.iterator();

        long start = System.currentTimeMillis();
        for (int doc = disiAll.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = disiAll.nextDoc()) {
            // do nothing
        }
        long end = System.currentTimeMillis();
        System.out.println("all cost: "+(end - start)+"ms");

        start = System.currentTimeMillis();
        for (int doc = disiFixBitSet.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = disiFixBitSet.nextDoc()) {
            // do nothing
        }
        end = System.currentTimeMillis();
        System.out.println("bit cost: "+(end - start)+"ms");
        System.out.println("---------------------------");
        }
    }
}
