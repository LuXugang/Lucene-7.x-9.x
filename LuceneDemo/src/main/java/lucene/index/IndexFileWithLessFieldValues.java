package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019-03-18 20:52
 */
public class IndexFileWithLessFieldValues {
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
    int count = 0;
    while (count++ < 23493) {
      Document doc = new Document();
      doc.add(new TextField("author", "aab b aab aabbcc ", Field.Store.YES));
      doc.add(new TextField("content", "a", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 1
      doc = new Document();
      doc.add(new TextField("author", "cd aab", Field.Store.YES));
      doc.add(new TextField("content", "b", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 2
      doc = new Document();
      doc.add(new TextField("author", "aab aabb aab", Field.Store.YES));
      doc.add(new TextField("content", "a", Field.Store.YES));
      indexWriter.addDocument(doc);
    }
    indexWriter.commit();
    // Per-top-reader state:
  }

  public static void main(String[] args) throws Exception{
    IndexFileWithLessFieldValues test = new IndexFileWithLessFieldValues();
    test.doIndex();
  }

}
