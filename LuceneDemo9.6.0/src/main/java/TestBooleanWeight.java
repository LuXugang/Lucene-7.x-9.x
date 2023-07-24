import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
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

public class TestBooleanWeight {
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
        addSegment(indexWriter, 1600);

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Query query1 = new TermQuery(new Term("name", new BytesRef("abc")));
        Query query2 = new TermQuery(new Term("name", new BytesRef("dabc")));
        Query queryNot = new TermQuery(new Term("name", new BytesRef("efg")));
        builder.add(new BooleanClause(query1, BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(query2, BooleanClause.Occur.SHOULD));
        builder.add(new BooleanClause(queryNot, BooleanClause.Occur.MUST_NOT));

        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.search(builder.build(), 100);


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
        TestBooleanWeight test = new TestBooleanWeight();
        test.doSearch();
    }
}
