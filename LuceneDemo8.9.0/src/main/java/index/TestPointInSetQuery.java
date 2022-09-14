package index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

public class TestPointInSetQuery {
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
        int a;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        while (count++ < 40960) {
            doc = new Document();
            a = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            min = Math.min(a, min);
            max = Math.max(a, max);
            doc.add(new IntPoint("sortField", a));
            doc.add(new NumericDocValuesField("sortField", a));
            doc.add(new BinaryDocValuesField("sortFieldString", new BytesRef(String.valueOf(a))));
            doc.add(new StringField("content", new BytesRef(String.valueOf(a)), Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        System.out.println("min: " + min + "");
        System.out.println("max: " + max + "");
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        int[] lowValue = {-1};
        int[] upValue = {70};
        int[] values = {20, 100};
        Query query = IntPoint.newSetQuery("sortField", values);
        ScoreDoc[] scoreDocs = searcher.search(query, 100).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档号: " + scoreDoc.doc + "");
        }

        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TestPointInSetQuery test = new TestPointInSetQuery();
        test.doSearch();
    }
}
