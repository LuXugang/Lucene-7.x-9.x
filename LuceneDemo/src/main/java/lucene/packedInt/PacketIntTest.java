package lucene.packedInt;

import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;

import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019-04-02 21:02
 */
public class PacketIntTest {
    public static void main(String[] args) {
        Random random = new Random();
        PackedLongValues.Builder builder = PackedLongValues.packedBuilder(PackedInts.COMPACT);
        long[] values = {69, 25, 261, 23};
        int count = 0;
        builder.add(1<< 9);
        while (count++ < 4){
            builder.add(random.nextInt(1 << 9));
        }
        builder.build();
    }
}
