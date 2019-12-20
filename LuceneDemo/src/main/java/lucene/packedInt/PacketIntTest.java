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
        int[] values1 = {1, 2, 4, 8, 12, 16, 20, 24, 28, 32, 40, 48, 56, 64};
        int count = 0;
        while (count++ < 1000000){
            builder.add(random.nextInt(1 << 13));
        }
        builder.build();
    }
}
