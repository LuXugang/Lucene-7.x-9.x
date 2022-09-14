package query;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

public class LRUCacheTest {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        int count = 0;
        int a, b;
        while (count < 40960) {
            doc = new Document();
            a = random.nextInt(100);
            b = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            b = b <= 2 ? b + 4 : b;
            doc.add(new IntPoint("number", a));
            doc.add(new NumericDocValuesField("number", b));
            doc.add(new BinaryDocValuesField("numberString", new BytesRef(String.valueOf(a))));
            doc.add(new StringField("termField", "good" + random.nextInt(3), Field.Store.YES));
            if (count % 2 == 0) {
                doc.add(new Field("content", "my good teach", fieldType));
            } else {
                doc.add(new Field("content", "my efds", fieldType));
            }
            doc.add(new Field("content", "ddf", fieldType));
            indexWriter.addDocument(doc);
            count++;
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = IntPoint.newRangeQuery("number", 1, 200);
        Query query1 =
                new TermRangeQuery("termField", new BytesRef("a"), new BytesRef("z"), true, true);
        Collector collector = new TotalHitCountCollector();

        // 返回Top5的结果
        int resultTopN = 100;
        int queryCount = 100;
        for (int i = 0; i < queryCount; i++) {
            searcher.search(query1, collector);
        }

        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        LRUCacheTest test = new LRUCacheTest();
        test.doSearch();
    }
}
