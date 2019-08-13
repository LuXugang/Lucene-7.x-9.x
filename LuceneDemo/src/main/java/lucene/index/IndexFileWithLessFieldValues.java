package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
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
    while (count++ < 1) {
      Document doc = new Document();
      doc.add(new TextField("author", "aab b aab aabbcc ", Field.Store.YES));
      doc.add(new TextField("content", "a b", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 1
      doc = new Document();
      doc.add(new TextField("author", "cd a", Field.Store.YES));
      doc.add(new TextField("content", "a b c h", Field.Store.YES));
      doc.add(new TextField("title", "d a", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 2
      doc = new Document();
      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
      doc.add(new TextField("content", "a c b e", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 4
      doc = new Document();
      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
      doc.add(new TextField("content", "b c e", Field.Store.YES));
      indexWriter.addDocument(doc);
    }
    indexWriter.commit();
    // Per-top-reader state:

    DirectoryReader reader = DirectoryReader.open(indexWriter);

    IndexSearcher indexSearcher = new IndexSearcher(reader);

    BooleanQuery.Builder builder = new BooleanQuery.Builder();

    builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "b")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "c")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "h")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "e")), BooleanClause.Occur.SHOULD);
    builder.setMinimumNumberShouldMatch(2);

    indexSearcher.search(builder.build(), 2);

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
