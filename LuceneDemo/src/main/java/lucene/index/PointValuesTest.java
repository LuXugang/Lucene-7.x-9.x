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
import java.util.Random;

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

    Random random = new Random();
    Document doc;
    // 0
    doc = new Document();
    doc.add(new IntPoint("content", 3, 5));
    doc.add(new IntPoint("title", 2, 5));
    indexWriter.addDocument(doc);
    // 1
    doc = new Document();
    doc.add(new IntPoint("content", 10, 55));
    doc.add(new IntPoint("title", 10, 55));
    indexWriter.addDocument(doc);

    int count = 0 ;
    while (count++ < 2048){
      doc = new Document();
      int a = random.nextInt(100);
      a = a == 0 ? a + 1 : a;
      int b = random.nextInt(100);
      b = b == 0 ? b + 1 : b;
      doc.add(new IntPoint("content", a , b));
      doc.add(new IntPoint("title", 10, 55));
      indexWriter.addDocument(doc);
    }


    indexWriter.commit();
  }

  public static void main(String[] args) throws Exception{
    PointValuesTest test = new PointValuesTest();
    test.doSearch();
//    byte[] packed = new byte[100];
//    IntPoint.encodeDimension(256, packed, 0);
//    System.out.println(packed);
  }
}
