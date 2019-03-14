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
    int count = 0;
//    while (count++ < 440000) {
//       0
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


    // 3
    doc = new Document();
    doc.add(new TextField("author", getMultiSamePrefixValue("ab"), Field.Store.YES));
    doc.add(new TextField("content", "c", Field.Store.YES));
    indexWriter.addDocument(doc);


    // 4
    doc = new Document();
    doc.add(new TextField("author", getMultiValue(), Field.Store.YES));
    doc.add(new TextField("content", "c", Field.Store.YES));
    indexWriter.addDocument(doc);

    // 5
    doc = new Document();
    doc.add(new TextField("author", getMultiSamePrefixValue("kb"), Field.Store.YES));
    doc.add(new TextField("content", "c", Field.Store.YES));
    indexWriter.addDocument(doc);

//    }
    indexWriter.commit();

    DirectoryReader  reader = DirectoryReader.open(indexWriter);
    IndexSearcher searcher = new IndexSearcher(reader);
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
//    builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.MUST);
    builder.add(new TermQuery(new Term("author", "a")), BooleanClause.Occur.MUST);
    Query query = builder.build();
    ScoreDoc[] scoreDoc = searcher.search(query, 100).scoreDocs;
    Document document  = reader.document(2);
    System.out.println(document.get("author"));
    System.out.println(scoreDoc.length);

    // Per-top-reader state:
  }

  public static String getSamePrefixRandomValue(String prefix){
    String str="abcdefghijklmnopqrstuvwxyz";
    Random random=new Random();
    StringBuffer sb=new StringBuffer();
    int length = getLength();
    for(int i=0;i<length;i++){
      int number=random.nextInt(25);
      sb.append(prefix);
      sb.append(str.charAt(number));
    }
    return sb.toString();
  }

  public static String getRandomValue(){
    String str="abcdefghijklmnopqrstuvwxyz";
    Random random=new Random();
    StringBuffer sb=new StringBuffer();
    int length = getLength();
    for(int i=0;i<length;i++){
      int number=random.nextInt(25);
      sb.append(str.charAt(number));
    }
    return sb.toString();
  }

  public static int getLength(){
    Random random = new Random();
    int length = random.nextInt(5);
    if (length < 3){
      length = length + 3;
    }
    return length;
  }

  public static String getMultiSamePrefixValue(String prefix){
    int valueCount = 0;
    StringBuilder stringBuilder = new StringBuilder();
    while (valueCount++ < 99){
      stringBuilder.append(getSamePrefixRandomValue(prefix));
      stringBuilder.append(" ");
    }
    stringBuilder.append("end");
    return stringBuilder.toString();
  }

  public static String getMultiValue(){
    int valueCount = 0;
    StringBuilder stringBuilder = new StringBuilder();
    while (valueCount++ < 99){
      stringBuilder.append(getRandomValue());
      stringBuilder.append(" ");
    }
    stringBuilder.append("end");
    return stringBuilder.toString();
  }

  public static void main(String[] args) throws Exception{
    IndexFileTest test = new IndexFileTest();
    test.doIndex();
  }
}
