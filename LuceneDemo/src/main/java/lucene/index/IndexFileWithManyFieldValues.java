package lucene.index;

import io.FileOperation;
import io.NativeFSLockFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019-02-21 09:58
 */
public class IndexFileWithManyFieldValues {
  private Directory directory;

  {
    try {
      FileOperation.deleteFile("./data");
      directory = FSDirectory.open(Paths.get("./data"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Analyzer analyzer = new WhitespaceAnalyzer();
  private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
  private IndexWriter indexWriter;

  public void doIndex() throws Exception {

    FieldType type = new FieldType();
    type.setStored(true);
    type.setStoreTermVectors(true);
    type.setStoreTermVectorPositions(true);
    type.setStoreTermVectorPayloads(true);
    type.setStoreTermVectorOffsets(true);
    type.setTokenized(true);
    type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

    conf.setUseCompoundFile(false);
    conf.setSoftDeletesField("myDeleteFiled");
    Sort sort = new Sort(new SortField("forSort", SortField.Type.STRING));
    conf.setIndexSort(sort);
    indexWriter = new IndexWriter(directory, conf);

    int count = 0;
    int n = 0;
    Document doc ;
    while (count++ < 1) {
//      doc.add(new Field("content", "abc", type));
//      doc.add(new Field("content", "cd", type));
//      doc.add(new StoredField("content", 3));
//      doc.add(new Field("author", "efg", type));

      // 文档0
      doc = new Document();
      doc.add(new SortedDocValuesField("forSort", new BytesRef("c")));
      doc.add(new Field("content", "a", type));
      doc.add(new IntPoint("abc", 3, 5, 9));
      indexWriter.addDocument(doc);

      // 文档1
      doc = new Document();
      doc.add(new SortedDocValuesField("forSort", new BytesRef("a")));
      doc.add(new Field("content", "b", type));
      doc.add(new IntPoint("abc", 3, 9, 9));
      indexWriter.addDocument(doc);

      // 文档2
      doc = new Document();
      doc.add(new SortedDocValuesField("forSort", new BytesRef("b")));
      doc.add(new Field("content", "c", type));
      indexWriter.addDocument(doc);


      indexWriter.deleteDocuments(new Term("content", "c"));

      // 文档3
      doc = new Document();
      doc.add(new Field("content", "a b c d", type));
      doc.add(new SortedDocValuesField("forSort", new BytesRef("d")));
      indexWriter.addDocument(doc);
      indexWriter.flush();
    }
//    indexWriter.commit();

    DirectoryReader  reader = DirectoryReader.open(indexWriter);
    IndexSearcher searcher = new IndexSearcher(reader);


    Query query = new TermQuery(new Term("content", "a"));
    ScoreDoc[] scoreDocs = searcher.search(query, 10).scoreDocs;

    Document document  = reader.document(2);
    System.out.println(document.get("content"));

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

  public static String getMultiSamePrefixValue(String prefix, int wordNum){
    int valueCount = 0;
    StringBuilder stringBuilder = new StringBuilder();
    while (valueCount++ < wordNum){
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
    IndexFileWithManyFieldValues test = new IndexFileWithManyFieldValues();
    test.doIndex();
  }
}
