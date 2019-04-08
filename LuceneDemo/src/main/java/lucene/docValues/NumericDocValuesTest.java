package lucene.docValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-02 22:39
 */
public class NumericDocValuesTest {
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

        String groupField1 = "a";
        String groupField2 = "b";
        // 0
        int count = 0;
        while (count++ < 1000) {
            Document doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 88L));
            doc.add(new NumericDocValuesField(groupField2, 10L));
            indexWriter.addDocument(doc);

            // 1
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 92));
//            doc.add(new TextField("abc", "value", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 2
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 4L));
            doc.add(new TextField("abc", "value", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 3
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 24L));
            doc.add(new TextField("abc", "value", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 4
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 20L));
            indexWriter.addDocument(doc);

            // 5
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 32L));
            indexWriter.addDocument(doc);

            // 6
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 42L));
            indexWriter.addDocument(doc);



        }

        indexWriter.commit();
    }

    public static void main(String[] args) throws Exception{
        NumericDocValuesTest test = new NumericDocValuesTest();
        test.doSearch();
    }
}
