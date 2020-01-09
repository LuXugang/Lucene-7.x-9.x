package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.InfoStream;

import java.io.IOException;
import java.nio.file.Paths;
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
      FileOperation.deleteFile("./data");
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
      conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
//      persistentSnapshotDeletionPolicy = new PersistentSnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy(), directory);
//      snapshotDeletionPolicy = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
//      conf.setIndexDeletionPolicy(persistentSnapshotDeletionPolicy);
//      conf.setIndexDeletionPolicy(snapshotDeletionPolicy);
      conf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
      conf.setMergePolicy(NoMergePolicy.INSTANCE);
//      conf.setMergePolicy(NoMergePolicy.INSTANCE);
      Supplier<Query> docsOfLast24Hours = () -> LongPoint.newRangeQuery("creation_date", 23, 48);
//      conf.setMergePolicy(new LogDocMergePolicy());
//      conf.setMergePolicy(new SoftDeletesRetentionMergePolicy("title", docsOfLast24Hours,
//              new LogDocMergePolicy()));
//      conf.setSoftDeletesField("docValuesField");
//      conf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
      InfoStream infoStream = InfoStream.NO_OUTPUT;
      conf.setMergedSegmentWarmer(new SimpleMergedSegmentWarmer(infoStream));
      conf.setCommitOnClose(false);
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
    // 文档0
    doc = new Document();
    doc.add(new StringField("author", "Lily", Field.Store.YES));
    doc.add(new StringField("title", "care", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", -2));
    indexWriter.addDocument(doc);

    // 文档1
    doc = new Document();
    doc.add(new StringField("author", "Lucy", Field.Store.YES));
    doc.add(new StringField("title", "notCare", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 2));
    indexWriter.addDocument(doc);

    doc = new Document();
    doc.add(new Field("author", "Jay", type));
    doc.add(new StringField("title", "totalnotCare", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 2));
    indexWriter.addDocument(doc);
    // 文档2
    doc = new Document();
    doc.add(new StringField("author", "Luxugang", Field.Store.YES));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    indexWriter.commit();

    doc = new Document();
    doc.add(new StringField("author", "Luxugang", Field.Store.YES));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    doc = new Document();
    doc.add(new StringField("author", "Luxugang", Field.Store.YES));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    indexWriter.commit();

    doc = new Document();
    doc.add(new StringField("author", "Luxugang", Field.Store.YES));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    doc = new Document();
    doc.add(new StringField("author", "Luxugang", Field.Store.YES));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    doc = new Document();
    doc.add(new StringField("author", "Luxugang", Field.Store.YES));
    doc.add(new StringField("title", "whatEver", Field.Store.YES));
    doc.add(new NumericDocValuesField("age", 0));
    indexWriter.addDocument(doc);
    indexWriter.commit();

    DirectoryReader reader = DirectoryReader.open(indexWriter);

    Query query = new MatchAllDocsQuery();
    IndexSearcher searcher = new IndexSearcher(reader);

    System.out.printf("abc");

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
