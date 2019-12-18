package lucene.packedInt;

import io.FileOperation;
import lucene.docValues.SortedDocValuesTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PackedLongValues;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019-04-02 21:02
 */
public class PacketIntTest {
    public static void main(String[] args) {
        Random random = new Random();
        PackedLongValues.Builder builder = PackedLongValues.packedBuilder(PackedInts.COMPACT);
        int count = 0;
        while (count++ < 1000000){
            builder.add(random.nextInt(1 << 13));
        }
        builder.build();
    }
}
