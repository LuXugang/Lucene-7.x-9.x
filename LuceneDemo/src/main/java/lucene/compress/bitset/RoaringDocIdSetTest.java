package lucene.compress.bitset;

import org.apache.lucene.util.RoaringDocIdSet;

/**
 * @author Lu Xugang
 * @date 2019/9/29 3:38 下午
 */
public class RoaringDocIdSetTest {
    public static void main(String[] args) {
        int maxDocId = 400000;
        RoaringDocIdSet.Builder builder = new RoaringDocIdSet.Builder(maxDocId);
        for (int i = 0; i < maxDocId; i++) {
            if(i == 3 || i == 7){
                continue;
            }
            builder.add(i);
        }
        RoaringDocIdSet roaringDocIdSet = builder.build();
        System.out.println("hha");
    }
}
