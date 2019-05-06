package lucene.query;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-30 17:50
 */
public class QueryBuilderTest {
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
//    while (count++ < 1700000) {
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
//    }
    indexWriter.commit();

    QueryBuilder builder = new QueryBuilder(new WhitespaceAnalyzer());
    builder.setEnableGraphQueries(true);
    Query query = builder.createPhraseQuery("author", "cd a");


    IndexReader reader = DirectoryReader.open(indexWriter);
    IndexSearcher searcher = new IndexSearcher(reader);
    ScoreDoc[]docs = searcher.search(query, 10).scoreDocs;
    // Per-top-reader state:
  }

  public static void main(String[] args) throws Exception{
    QueryBuilderTest  test = new QueryBuilderTest();
    test.doIndex();
  }

}
