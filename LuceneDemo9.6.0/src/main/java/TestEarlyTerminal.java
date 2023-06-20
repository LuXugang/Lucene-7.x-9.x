import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NamedThreadFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestEarlyTerminal {
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
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        int count = 0;
        // segment 0
        while (count++ != 310000){
            doc = new Document();
            doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
            doc.add(new StringField("name", String.valueOf(random.nextInt(1000000)), StringField.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();

        count = 0;
        // segment 3
        while (count++ != 100000){
            doc = new Document();
            doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
            doc.add(new StringField("name", String.valueOf(random.nextInt(1000000)), StringField.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();

        count = 0;
        // segment 2
        while (count++ != 250000){
            doc = new Document();
            doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
            doc.add(new StringField("name", String.valueOf(random.nextInt(1000000)), StringField.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();

        count = 0;
        // segment 3
        while (count++ != 10000){
            doc = new Document();
            doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
            doc.add(new StringField("name", String.valueOf(random.nextInt(1000000)), StringField.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        // four segments
        assert  reader.leaves().size() == 4;

        ThreadPoolExecutor service =
                new ThreadPoolExecutor(
                        4,
                        4,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(),
                        new NamedThreadFactory("TestIndexSearcher"));
        IndexSearcher multiThreadsSearcher = new IndexSearcher(reader, service);
        IndexSearcher singleThreadSearcher = new IndexSearcher(reader);
        Query query = new MatchAllDocsQuery();
        final int topN = 2000;

        // multi threads search
        final int slicesSize = multiThreadsSearcher.getSlices().length;
        assert slicesSize == 3;
        CollectorManager<TopScoreDocCollector, TopDocs> collectorManager = TopScoreDocCollector.createSharedManager(topN, null, topN);
        TopDocs topDocs1 = multiThreadsSearcher.search(query, collectorManager);
        // totalHits == 6000
        assert topDocs1.totalHits.value == ((long) topN * slicesSize);

        // single thread search
        TopDocs topDocs2 = singleThreadSearcher.search(query, topN);
        // totalHits == 2001
        assert topDocs2.totalHits.value == 2001;

        System.out.println("wait a moment");
        Thread.sleep(10000);
        service.shutdown();
        indexWriter.close();
        reader.close();
        directory.close();
        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TestEarlyTerminal test = new TestEarlyTerminal();
        test.doSearch();
    }
}
