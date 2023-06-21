import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.NamedThreadFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//TODO: 文章中的例子，不要修改
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


    public void doSearch() throws Exception {
        IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(new WhitespaceAnalyzer()));
        addSegment(indexWriter, 310000);
        addSegment(indexWriter, 100000);
        addSegment(indexWriter, 240000);
        addSegment(indexWriter, 1000);
        addSegment(indexWriter, 1400);
        addSegment(indexWriter, 1500);
        addSegment(indexWriter, 1300);
        addSegment(indexWriter, 1200);
        addSegment(indexWriter, 1600);

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        ThreadPoolExecutor service =
                new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("TestIndexSearcher"));
        IndexSearcher multiThreadsSearcher = new IndexSearcher(reader, service);
        IndexSearcher singleThreadSearcher = new IndexSearcher(reader);
        Query query = new MatchAllDocsQuery();
        final int topN = 1000;
        CollectorManager<TopScoreDocCollector, TopDocs> collectorManager = TopScoreDocCollector.createSharedManager(topN, null, topN);
        // 索引中有9个段
        assert reader.leaves().size() == 9;
        // 使用多线程的IndexSearcher中有4个Slice
        assert multiThreadsSearcher.getSlices().length == 4;

        // 多线程查询
        TopDocs topDocs1 = multiThreadsSearcher.search(query, collectorManager);
        System.out.println("多线程中，Collect中处理的文档数量: "+topDocs1.totalHits.value);

        // 单线程查询
        TopDocs topDocs2 = singleThreadSearcher.search(query, topN);
        System.out.println("单线程中，Collect中处理的文档数量: " + topDocs2.totalHits.value);

        System.out.println("wait a moment");
        Thread.sleep(10000);
        service.shutdown();
        indexWriter.close();
        reader.close();
        directory.close();
        System.out.println("DONE");
    }

    void addSegment(IndexWriter indexWriter, int documentSize) throws Exception{
        Document doc;
        int count = 0;
        Random random = new Random();
        while (count++ != documentSize){
            doc = new Document();
            doc.add(new StringField("name", String.valueOf(random.nextInt(1000000)), StringField.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
    }

    public static void main(String[] args) throws Exception {
        TestEarlyTerminal test = new TestEarlyTerminal();
        test.doSearch();
    }
}
