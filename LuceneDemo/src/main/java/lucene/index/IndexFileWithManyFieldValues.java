package lucene.index;

import io.FileOperation;
import io.NativeFSLockFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;

import javax.print.Doc;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Lu Xugang
 * @date 2019-02-21 09:58
 */
public class IndexFileWithManyFieldValues {
  private Directory directory ;
  private Directory directory2;
  private Directory directory3;
  private Analyzer analyzer = new WhitespaceAnalyzer();
  private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
  private IndexWriter indexWriter;

  {
    try {
      FileOperation.deleteFile("./data");
//      FileOperation.deleteFile("./data1");
//      directory3 = FSDirectory.open(Paths.get("./data01"));
//      directory2 = FSDirectory.open(Paths.get("./data02"));
//      Set<String> primaryExtensions = new HashSet<>();
//      primaryExtensions.add("fdx");
//      primaryExtensions.add("fdt");
//      primaryExtensions.add("nvd");
//      primaryExtensions.add("nvm");
//      directory = new FileSwitchDirectory(primaryExtensions, directory3, directory2, true);
      directory = FSDirectory.open(Paths.get("./data"));
      conf.setUseCompoundFile(false);
      conf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
      conf.setSoftDeletesField("myDeleteFiled");
      conf.setMergePolicy(new SoftDeletesRetentionMergePolicy(conf.getSoftDeletesField(), MatchAllDocsQuery::new, NoMergePolicy.INSTANCE) );
      conf.setRAMBufferSizeMB(10);
      indexWriter = new IndexWriter(directory, conf);
//      directory = new NIOFSDirectory(Paths.get("./data"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  public void doIndex() throws Exception {

    FieldType type = new FieldType();
    type.setStored(true);
    type.setStoreTermVectors(true);
    type.setStoreTermVectorPositions(true);
    type.setStoreTermVectorPayloads(true);
    type.setStoreTermVectorOffsets(true);
    type.setTokenized(true);
    type.setIndexOptions(IndexOptions.DOCS);



    int count = 0;
    int n = 0;
    while (count++ < 1) {
      Document doc  = new Document();
      doc.add(new Field("content", "abc", type));
      doc.add(new Field("content", "a", type));
      doc.add(new StoredField("content", 3));
      doc.add(new Field("author", "efg", type));
      indexWriter.addDocument(doc);

      doc = new Document();
      doc.add(new StringField("newField", "newFieldValue", Field.Store.YES));
      indexWriter.addDocument(doc);
      if(Thread.currentThread().getName().equals("ThreadAAAA")){
        System.out.println("sleep");
        Thread.sleep(10000);
        System.out.println("sleep");
      }
//      indexWriter.updateDocument(new Term("content", "a"), doc);

      // 文档0
//      doc = new Document();
//      doc.add(new NumericDocValuesField("age", 0));
//      doc.add(new StringField("content", getRandomValue(), Field.Store.YES));
//      doc.add(new IntPoint("coordinate", 3, 5, 9));
//      indexWriter.addDocument(doc);

//       文档1
//      doc = new Document();
//      doc.add(new SortedDocValuesField("forSort", new BytesRef("a")));
//      doc.add(new Field("content", "b", type));
//      doc.add(new IntPoint("abc", 3, 9, 9));
//      indexWriter.addDocument(doc);

//       文档2
//      doc = new Document();
//      doc.add(new NumericDocValuesField("abc", 0));
//      doc.add(new Field("author", "Shakespeare", type));
//      indexWriter.addDocument(doc);

//      indexWriter.deleteDocuments(new Term("content", "abc"));
//
      indexWriter.deleteDocuments(new TermQuery(new Term("content", "a")));
//
//      indexWriter.deleteAll();

      doc.add(new Field("content", "abc", type));
      doc.add(new Field("content", "a", type));
      doc.add(new StoredField("content", 3));
      doc.add(new Field("author", "efg", type));
      indexWriter.addDocument(doc);


      doc = new Document();
      doc.add(new StringField("content", "abc", Field.Store.YES));
      indexWriter.updateDocument(new Term("content", "abc"), doc);


        System.out.println("current ThreadName is "+Thread.currentThread().getName()+"");
        if(Thread.currentThread().getName().equals("ThreadAAAA")){
          indexWriter.flush();
          break;
        }
        indexWriter.flush();
        return;
//
//
//
//      List<Document> documents = new ArrayList<>();
//      doc = new Document();
//      doc.add(new TextField("content", "random text", Field.Store.YES));
//      documents.add(doc);
//
//      doc = new Document();
//      doc.add(new TextField("content", "random name", Field.Store.YES));
//      documents.add(doc);
//
//      indexWriter.updateDocuments(new Term("author", "Shakespeare"), documents);

//      indexWriter.updateNumericDocValue(new Term("author", "Shakespeare"), "age", 23);
//
//      indexWriter.updateBinaryDocValue(new Term("subject", "Calculus"), "inventor", new BytesRef("Leibniz"));

//      Field[] fields= new Field[2];
//      fields[0] = new NumericDocValuesField("age", 23);
//      fields[1] = new BinaryDocValuesField("inventor", new BytesRef("Leibniz"));
//      indexWriter.updateDocValues(new Term("author", "Shakespeare"), fields);

//      indexWriter.softUpdateDocument(new Term("content", "a"), doc, new NumericDocValuesField(conf.getSoftDeletesField(), 223));
//      indexWriter.commit();


//      indexWriter.deleteDocuments(new Term("content", "b"));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.deleteDocuments(new Term("content", getRandomValue()));
//      indexWriter.updateDocValues(new Term("content", "c"), new NumericDocValuesField("文档2", 3));
//      indexWriter.updateDocValues(new Term("content", "a"), new NumericDocValuesField("文档0", 4));

      // 文档3
//      doc = new Document();
//      doc.add(new Field("content", "d", type));
//      doc.add(new BinaryDocValuesField("myDocValues", new BytesRef("d")));
//      indexWriter.addDocument(doc);
//      indexWriter.flush();
//      indexWriter.updateDocValues(new Term("content", "d"), new BinaryDocValuesField("文档3", new BytesRef("e")));

//     if(count % 10 == 0){
//       indexWriter.commit();
//     }

    }
//    Map<String, String> userData = new HashMap<>();
//    userData.put("1", "abc");
//    userData.put("2", "efd");
//    indexWriter.setLiveCommitData(userData.entrySet());
    if(Thread.currentThread().getName().equals("ThreadAAAA")){
      indexWriter.commit();
      System.out.println("hah");
    }


    DirectoryReader  reader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);

    SoftDeletesDirectoryReaderWrapper readerWrapper = new SoftDeletesDirectoryReaderWrapper(reader, conf.getSoftDeletesField());



    Query query = new TermQuery(new Term("content", "a"));
    ScoreDoc[] scoreDocs = searcher.search(query, 10).scoreDocs;

    Document document  = reader.document(2);
    System.out.println("DONE");

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
//    return 200000;
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

  public static class MyThread implements Runnable{
    IndexFileWithManyFieldValues test;

    public MyThread(IndexFileWithManyFieldValues test) {
      this.test = test;
    }

    @Override
    public void run(){
      try {
        test.doIndex();
        System.out.println("DONE current ThreadName is "+Thread.currentThread().getName()+"");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) throws Exception{
    IndexFileWithManyFieldValues test = new IndexFileWithManyFieldValues();

    MyThread a = new MyThread(test);
    MyThread b = new MyThread(test);

    Thread t1 = new Thread(a, "ThreadAAAA");
    Thread t2 = new Thread(b, "ThreadBBBB");

    System.out.println("a start to run");
    t1.setDaemon(true);
    System.out.println(""+t1.isDaemon()+"");
    t1.start();
    System.out.println("b start to run");
    t2.setDaemon(true);
    System.out.println(""+t2.isDaemon()+"");
    t2.start();
    t1.join();
    t2.join();
  }
}
