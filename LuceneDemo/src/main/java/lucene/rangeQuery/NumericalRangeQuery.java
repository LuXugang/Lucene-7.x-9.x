package lucene.rangeQuery;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-02-25 14:22
 */
public class NumericalRangeQuery {
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
//
//    // 0
//    Document doc = new Document();
//    doc.add(new IntPoint("point", -3));
//    indexWriter.addDocument(doc);
//
//    // 1
//    doc = new Document();
//    doc.add(new IntPoint("point", -9));
//    indexWriter.addDocument(doc);
//
//    // 2
//    doc = new Document();
//    doc.add(new IntPoint("point", 11));
//    indexWriter.addDocument(doc);
//
//
//    // 3
//    doc = new Document();
//    doc.add(new IntPoint("point", 1));
//    indexWriter.addDocument(doc);
//
//    indexWriter.commit();

    DirectoryReader r = DirectoryReader.open(indexWriter);
    IndexSearcher s = new IndexSearcher(r);

    int num;
    num = s.count(IntPoint.newRangeQuery("point", -9, 1));
    System.out.println("result number : "+ num +"");


    // Per-top-reader state:k
  }



  public static void main(String[] args) throws Exception{
    NumericalRangeQuery query = new NumericalRangeQuery();
    query.doIndex();
  }
}
