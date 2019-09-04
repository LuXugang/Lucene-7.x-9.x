package lucene.query;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019-05-05 16:40
 */
public class LatLonPointDistanceQueryTest {
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

    Random random = new Random();
    Document doc;
    // 0
    doc = new Document();
    doc.add(new LatLonPoint("content", 0, 0));
    indexWriter.addDocument(doc);
    // 1
    doc = new Document();
    doc.add(new LatLonPoint("content", 90, 180));
    indexWriter.addDocument(doc);


    doc = new Document();
//    doc.add(new DoublePoint("title", 10, 55));
    indexWriter.addDocument(doc);

    int count = 0 ;
    while (count++ < 2048){
      doc = new Document();
      double a = random.nextDouble();
      int a1 = random.nextInt(90);
      a = a > 90 ? a - 1 : a;
      a = a + a1;
      double b = random.nextDouble();
      b = b > 180 ? b - 1 : b;
      int b1 = random.nextInt(180);
      b = b + b1;
//      System.out.println(a);
//      System.out.println(b);
//      System.out.println("-------");
      doc.add(new LatLonPoint("content", a , b));
      indexWriter.addDocument(doc);
    }

    indexWriter.commit();
    DirectoryReader r = DirectoryReader.open(indexWriter);
    IndexSearcher s = new IndexSearcher(r);


    Query query = LatLonPoint.newDistanceQuery("content", 20, 30, 1000000);
    TopDocs docs = s.search(query, 100);

    System.out.println("result number : "+ docs.totalHits+"");


    // Per-top-reader state:k
  }



  public static void main(String[] args) throws Exception{
    LatLonPointDistanceQueryTest test = new LatLonPointDistanceQueryTest();
    test.doIndex();
  }
}
