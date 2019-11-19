package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.InfoStream;
import org.apache.lucene.util.fst.NoOutputs;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.Supplier;

/**
 * @author Lu Xugang
 * @date 2019-02-21 09:58
 */
public class IndexFileWithManyFieldValues {
  private Directory directory ;
  private Directory directory2;
  private Directory directory3;
  private Analyzer analyzer = new CJKAnalyzer();
  private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
  private IndexWriter indexWriter;
  private PersistentSnapshotDeletionPolicy persistentSnapshotDeletionPolicy;
  private SnapshotDeletionPolicy snapshotDeletionPolicy;

  {
    try {
//      FileOperation.deleteFile("./data2");
//      FileOperation.deleteFile("./data");
//      directory3 = FSDirectory.open(Paths.get("./data01"));
//      directory2 = FSDirectory.open(Paths.get("./data02"));
//      Set<String> primaryExtensions = new HashSet<>();
//      primaryExtensions.add("fdx");
//      primaryExtensions.add("fdt");
//      primaryExtensions.add("nvd");
//      primaryExtensions.add("nvd");
//      primaryExtensions.add("nvm");
//      directory = new FileSwitchDirectory(primaryExtensions, directory3, directory2, true);
      directory = FSDirectory.open(Paths.get("./data"));
//      directory = FSDirectory.open(Paths.get("./data1"));
      conf.setUseCompoundFile(true);
//      conf.setSoftDeletesField("title");
      conf.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
//      persistentSnapshotDeletionPolicy = new PersistentSnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy(), directory);
//      snapshotDeletionPolicy = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
//      conf.setIndexDeletionPolicy(persistentSnapshotDeletionPolicy);
//      conf.setIndexDeletionPolicy(snapshotDeletionPolicy);
//      conf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
//      conf.setMergePolicy(NoMergePolicy.INSTANCE);
      Supplier<Query> docsOfLast24Hours = () -> LongPoint.newRangeQuery("creation_date", 23, 48);
//      conf.setMergePolicy(new LogDocMergePolicy());
//      conf.setMergePolicy(new SoftDeletesRetentionMergePolicy("title", docsOfLast24Hours,
//              new LogDocMergePolicy()));
//      conf.setSoftDeletesField("docValuesField");
//      conf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
      InfoStream infoStream = InfoStream.NO_OUTPUT;
      conf.setMergedSegmentWarmer(new SimpleMergedSegmentWarmer(infoStream));
      SortField indexSortField = new SortField("age", SortField.Type.LONG);
      Sort indexSort = new Sort(indexSortField);;
//    conf.setIndexSort(indexSort);
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
    type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);



    int count = 0;
    Document doc;
    while (count++ < 1) {
      // 文档0
      doc = new Document();
      doc.add(new Field("author", "国'人", type));
      doc.add(new Field("title", "notCare", type));
      doc.add(new NumericDocValuesField("age", -2));
      indexWriter.addDocument(doc);
      // 文档1
      doc = new Document();
      doc.add(new Field("author", "Lily", type));
      doc.add(new StringField("title", "Care", Field.Store.YES));
      doc.add(new NumericDocValuesField("age", 2));
      indexWriter.addDocument(doc);

      // 文档2
      doc = new Document();
      doc.add(new Field("author", "Luxugang", type));
      doc.add(new StringField("title", "whatEver", Field.Store.YES));
      doc.add(new NumericDocValuesField("age", 0));
      indexWriter.addDocument(doc);

//      indexWriter.deleteDocuments(new Term("author", "Luxugang"));

      indexWriter.commit();
    }
    indexWriter.commit();
    DirectoryReader  reader = DirectoryReader.open(indexWriter);
    indexWriter.close();
    indexWriter = null;
    IndexWriterConfig newConf = new IndexWriterConfig(analyzer);
    newConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    indexWriter = new IndexWriter(directory, newConf);
    doc = new Document();
    doc.add(new Field("author", "Luxugang", type));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    indexWriter.commit();

//    indexWriter.updateNumericDocValue(new Term("author", "Luxugang"), "age", 3);
//    DirectoryReader oldReader = DirectoryReader.open(FSDirectory.open(Paths.get("./data2")));
//    CodecReader[] readers = new CodecReader[oldReader.leaves().size()];
//    for (int i = 0; i < readers.length; i++) {
//      readers[i] = (CodecReader)oldReader.leaves().get(i).reader();
//    }
//    indexWriter.addIndexes(readers);


//      persistentSnapshotDeletionPolicy.snapshot();
//    Map<String, String> userData = new HashMap<>();
//    userData.put("1", "abc");
//    userData.put("2", "efd");
//    indexWriter.setLiveCommitData(userData.entrySet());
//    System.out.println(""+Thread.currentThread().getName()+" start to sleep");
//    Thread.sleep(1000000000);
//    indexWriter.flush();

//    DirectoryReader  reader = DirectoryReader.open(directory);
//    DirectoryReader  reader = DirectoryReader.open(indexWriter, true, true);

//    reader = DirectoryReader.openIfChanged(reader);
//    reader = DirectoryReader.openIfChanged(reader);

//    DirectoryReader reader1 = new ExitableDirectoryReader(reader, new QueryTimeoutImpl(2000));
//
    IndexSearcher searcher = new IndexSearcher(reader);
    Query query = new MatchAllDocsQuery();
//
//    SortField sortField  = new SortedNumericSortField("docValuesField", SortField.Type.INT);
//
//    Sort sort = new Sort(sortField);
//
    ScoreDoc[] scoreDocs = searcher.search(query, 10).scoreDocs;
    for (int i = 0; i < scoreDocs.length; i++) {
      ScoreDoc scoreDoc = scoreDocs[i];
      // 输出满足查询条件的 文档号
      System.out.println("result"+i+": 文档"+scoreDoc.doc+"");
    }
    // Per-top-reader state:
//
////   reader = DirectoryReader.openIfChanged(reader);

    System.out.println("hah");
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

//    System.out.println("a start to run");
//    t1.setDaemon(true);
//    System.out.println(""+t1.isDaemon()+"");
//    t1.start();
//    System.out.println("b start to run");
//    t2.setDaemon(true);
//    System.out.println(""+t2.isDaemon()+"");
//    t2.start();
//    t1.join();
//    t2.join();
    test.doIndex();
  }
}
