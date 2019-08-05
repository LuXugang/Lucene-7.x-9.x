package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    while (count++ < 1700000) {
      Document doc = new Document();
      doc.add(new TextField("author", "aab b aab aabbcc ", Field.Store.YES));
      doc.add(new TextField("content", "a", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 1
      doc = new Document();
      doc.add(new TextField("author", "cd a", Field.Store.YES));
      doc.add(new TextField("content", "c", Field.Store.YES));
      doc.add(new TextField("title", "d a", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 2
      doc = new Document();
      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
      doc.add(new TextField("content", "a", Field.Store.YES));
      indexWriter.addDocument(doc);
    }
    indexWriter.commit();
    // Per-top-reader state:

  }

  public static void main(String[] args) throws Exception{
    IndexFileWithLessFieldValues test = new IndexFileWithLessFieldValues();
    test.doIndex();
//    FileChannel         channel = FileChannel.open(Paths.get("/Users/luxugang/project/Lucene-7.5.0/LuceneDemo/data/write.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//    FileLock fileLock = channel.tryLock();
//    System.out.printf("abc");
//    System.out.printf("abc");
//    System.out.printf("abc");
//    fileLock.release();
//    FileLock fileLock1 = FileChannel.open(Paths.get("/Users/luxugang/project/Lucene-7.5.0/LuceneDemo/data/write.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).lock();
//    System.out.printf("abc");
  }

}
