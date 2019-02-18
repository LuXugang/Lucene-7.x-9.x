package lucene.utils;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.StringHelper;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-02-18 09:54
 */
public class BytesRefHashTest {
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

    private void doSearch() throws Exception{
        FieldType customType = new FieldType();
//        customType.setStored(true);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        String groupField = "superStart";
        // 0
        Document doc = new Document();
        doc.add(new SortedDocValuesField(groupField, new BytesRef("mop")));
        indexWriter.addDocument(doc);

        // 1
        doc = new Document();
        doc.add(new SortedDocValuesField(groupField, new BytesRef("moth")));
        indexWriter.addDocument(doc);

        // 2
        doc = new Document();
        doc.add(new SortedDocValuesField(groupField, new BytesRef("of")));
        indexWriter.addDocument(doc);

        // 3
        doc = new Document();
        doc.add(new SortedDocValuesField(groupField, new BytesRef("star")));
        indexWriter.addDocument(doc);
        indexWriter.commit();

    }

    private void addGroupField(Document doc, String groupField, String value) {
        doc.add(new SortedDocValuesField(groupField, new BytesRef(value)));
    }

    public static void main(String[] args) throws Exception{
      BytesRefHashTest test = new BytesRefHashTest();
      test.doSearch();
    }


}
