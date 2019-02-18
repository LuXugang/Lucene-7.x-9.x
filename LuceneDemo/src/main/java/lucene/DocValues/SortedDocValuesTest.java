package lucene.DocValues;

import io.FileOperation;
import lucene.facet.SortedSetDocValuesFacetsTest;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-02-18 15:03
 */
public class SortedDocValuesTest {
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

    String groupField = "superStart";
    // 0
    Document doc = new Document();
    doc.add(new SortedDocValuesField(groupField, new BytesRef("aa")));
    indexWriter.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new SortedDocValuesField(groupField, new BytesRef("cc")));
    indexWriter.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new SortedDocValuesField(groupField, new BytesRef("bb")));
    indexWriter.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new SortedDocValuesField(groupField, new BytesRef("ff")));
    indexWriter.addDocument(doc);

    // 4
    doc = new Document();
    doc.add(new TextField("abc", "value", Field.Store.YES));
    indexWriter.addDocument(doc);

    indexWriter.commit();
  }

  public static void main(String[] args) throws Exception{
    SortedDocValuesTest SortedDocValuesTest = new SortedDocValuesTest();
    SortedDocValuesTest.doSearch();
  }
}
