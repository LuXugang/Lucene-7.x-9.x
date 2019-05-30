package lucene.index;


import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.RamUsageEstimator;

/**
 * @author Lu Xugang
 * @date 2019-05-24 15:41
 */
public class DocsWithFieldSetTest {


    private FixedBitSet set;
    private int cost = 0;
    private int lastDocId = -1;

    void add(int docID) {
        if (docID <= lastDocId) {
            throw new IllegalArgumentException("Out of order doc ids: last=" + lastDocId + ", next=" + docID);
        }
        if (set != null) {
            set = FixedBitSet.ensureCapacity(set, docID);
            set.set(docID);
        } else if (docID != cost) {
            // migrate to a sparse encoding using a bit set
            set = new FixedBitSet(docID + 1);
            set.set(0, cost);
            set.set(docID);
        }
        lastDocId = docID;
        cost++;
    }

    public DocIdSetIterator iterator() {
        return set != null ? new BitSetIterator(set, cost) : DocIdSetIterator.all(cost);
    }


    public static void main(String[] args) {

        DocsWithFieldSetTest test = new DocsWithFieldSetTest();
        test.add(0);
        test.add(2);
        test.add(6);
        test.add(9);
        test.add(11);


    }
}
