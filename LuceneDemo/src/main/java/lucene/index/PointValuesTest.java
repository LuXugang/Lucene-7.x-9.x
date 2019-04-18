package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-18 15:43
 */
public class PointValuesTest {

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

    Document doc;
    // 0
    doc = new Document();
    doc.add(new IntPoint("content", 3, 5, 9));
    doc.add(new IntPoint("content", 1, 2, 8));
    indexWriter.addDocument(doc);
    // 1
    doc = new Document();
    doc.add(new IntPoint("content", 10, 55, 23));
    indexWriter.addDocument(doc);
    indexWriter.commit();
  }

  public static void main(String[] args) throws Exception{
    PointValuesTest test = new PointValuesTest();
    test.doSearch();
  }
}
