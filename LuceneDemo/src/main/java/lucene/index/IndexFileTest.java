package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-02-21 09:58
 */
public class IndexFileTest {
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

  public void doIndex() throws Exception {
    conf.setUseCompoundFile(false);
    indexWriter = new IndexWriter(directory, conf);

    // 0
    Document doc = new Document();
    doc.add(new TextField("content", "a b a", Field.Store.YES));
    doc.add(new TextField("author", "a", Field.Store.YES));
    indexWriter.addDocument(doc);

    // 1
    doc = new Document();
    doc.add(new TextField("content", "c a", Field.Store.YES));
    doc.add(new TextField("author", "b", Field.Store.YES));
    indexWriter.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new TextField("content", "a a", Field.Store.YES));
    doc.add(new TextField("author", "a", Field.Store.YES));
    indexWriter.addDocument(doc);

    indexWriter.commit();

    // Per-top-reader state:
  }

  public static void main(String[] args) throws Exception{
    IndexFileTest test = new IndexFileTest();
    test.doIndex();
  }
}
