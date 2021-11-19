package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestPointRangeQuery {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Analyzer analyzer = new WhitespaceAnalyzer();
    private final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        Document doc;
        int count = 0 ;
        int a, b;
        int minA = Integer.MAX_VALUE;
        int maxA = Integer.MIN_VALUE ;
        int minB = Integer.MAX_VALUE;
        int maxB = Integer.MIN_VALUE ;
        while (count++ < 2000){
            doc = new Document();
            a = random.nextInt(100);
            b = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            b = b <= 2 ? a + b : a;
            minA = Math.min(a, minA);
            maxA = Math.max(a, maxA);
            minB = Math.min(b, minB);
            maxB = Math.max(b, maxB);
            doc.add(new IntPoint("number", a, b));
            doc.add(new NumericDocValuesField("sortField", a));
            doc.add(new BinaryDocValuesField("sortFieldString", new BytesRef(String.valueOf(a))));
            doc.add(new StringField("content", new BytesRef(String.valueOf(a)), Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        System.out.println("minA minB: "+minA+" "+minB+"");
        System.out.println("maxA maxB: "+maxA+" "+maxB+"");

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        int [] lowValue = {2, 2};
        int [] upValue = {80, 80};
        Query query = IntPoint.newRangeQuery("number", lowValue, upValue);
        ScoreDoc[] scoreDocs = searcher.search(query, 100).scoreDocs;
        System.out.println("match count: "+scoreDocs.length+"");
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档号: "+scoreDoc.doc+"");
        }

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        TestPointRangeQuery test = new TestPointRangeQuery();
        test.doSearch();
    }
}
