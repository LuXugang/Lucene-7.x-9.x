package index;

import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * @author Lu Xugang
 * @date 2021/7/8 4:58 下午
 */
public class TestLineReader {
    public static void main(String[] args) throws IOException {
        Directory directory = new NIOFSDirectory(Paths.get("./data"));
        DirectoryReader ireader = DirectoryReader.open(directory);
        long startTime = 0;
        long endTime = 0;
        int baseDoc = 0;
        for (int k = 0; k < 10; k++) {
            startTime += System.currentTimeMillis();
            for (LeafReaderContext context : ireader.leaves()) {
                SortedNumericDocValues docValuesLong =
                        DocValues.getSortedNumeric(context.reader(), "long");
                SortedNumericDocValues docValuesLong300 =
                        DocValues.getSortedNumeric(context.reader(), "long300");
                SortedNumericDocValues docValuesLong3000 =
                        DocValues.getSortedNumeric(context.reader(), "long3000");
                SortedNumericDocValues docValuesLong30000 =
                        DocValues.getSortedNumeric(context.reader(), "long30000");
                SortedNumericDocValues docValuesLong300000 =
                        DocValues.getSortedNumeric(context.reader(), "long300000");
                SortedNumericDocValues docValuesLong3000000 =
                        DocValues.getSortedNumeric(context.reader(), "long3000000");
                for (int i = 0; i < context.reader().maxDoc(); i++) {
                    docValuesLong.nextDoc();
                    docValuesLong.nextValue();
                    docValuesLong300.nextDoc();
                    docValuesLong300.nextValue();
                    docValuesLong3000.nextDoc();
                    docValuesLong3000.nextValue();
                    //                    docValuesLong30000.nextDoc();
                    //                    docValuesLong30000.nextValue();
                    //                    docValuesLong300000.nextDoc();
                    //                    docValuesLong300000.nextValue();
                    //                    docValuesLong3000000.nextDoc();
                    //                    docValuesLong3000000.nextValue();
                }
                baseDoc += context.reader().maxDoc();
            }
            endTime += System.currentTimeMillis();
            System.out.println(k);
        }
        System.out.println(baseDoc);
        System.out.println("line");
        System.out.println((endTime - startTime) / 10);
    }
}
