import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.VectorUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestTieredMergePolicy {
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
        //
        int count = 0;

        doc = new Document();
        doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
        doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
        doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("b")));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("b")));
        doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("b")));
        indexWriter.addDocument(doc);

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        System.out.println("DONE");
    }

    private float[] randomVector(int dim, Random random) {
        float[] v = new float[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = random.nextFloat();
        }
        VectorUtil.l2normalize(v);
        return v;
    }

    public static void main(String[] args) throws Exception {
        TestTieredMergePolicy test = new TestTieredMergePolicy();
        test.doSearch();
    }
}
