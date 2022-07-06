import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.StringDocValuesReaderState;
import org.apache.lucene.facet.StringValueFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.VectorUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestKNNQuery {
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
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Document doc;
        doc = new Document();
        doc.add(new KnnVectorField("vector", new float[]{1.0f, 0.3f}));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new KnnVectorField("vector", new float[]{1.0f, 0.4f}));
        indexWriter.addDocument(doc);


        doc = new Document();
        doc.add(new KnnVectorField("vector", new float[]{1.0f, 0.3f}));
        indexWriter.addDocument(doc);


        indexWriter.commit();

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        float[] queryFloat = new float[]{1.0f, 0.2f};

        KnnVectorQuery kvq = new KnnVectorQuery("vector", queryFloat, 10);

        ScoreDoc[] docs = searcher.search(kvq, 1000).scoreDocs;
        System.out.println("match: "+docs.length+"");
        for (int i = 0; i < docs.length; i++) {
            System.out.println(docs[i].doc);
        }




    }
    public static void main(String[] args) throws Exception{
        TestKNNQuery test = new TestKNNQuery();
        test.doSearch();
    }
}
