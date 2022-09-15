import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.VectorUtil;

public class TestSortedNumericDocValuesCount {
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
        Random random = new Random();
        Document doc;
        //
        int count = 0;

        doc = new Document();
        doc.add(new SortedNumericDocValuesField("number", 100));
        doc.add(new SortedNumericDocValuesField("number", 100));
        doc.add(new SortedNumericDocValuesField("number", 200));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new SortedNumericDocValuesField("number", 1));
        doc.add(new SortedNumericDocValuesField("number", 1));
        indexWriter.addDocument(doc);

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        for (LeafReaderContext leaf : reader.leaves()) {
            SortedNumericDocValues docValues = leaf.reader().getSortedNumericDocValues("number");
            for (int doc1 = docValues.nextDoc();
                    doc1 != DocIdSetIterator.NO_MORE_DOCS;
                    doc1 = docValues.nextDoc()) {
                System.out.println("abc");
                System.out.println("docValuesCount:" + docValues.docValueCount());
            }
        }
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
        TestSortedNumericDocValuesCount test = new TestSortedNumericDocValuesCount();
        test.doSearch();
    }
}
