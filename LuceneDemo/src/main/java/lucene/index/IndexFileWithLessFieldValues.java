package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

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

  private IndexWriter indexWriter;

  public void doIndex() throws Exception {

    Analyzer analyzer = new WhitespaceAnalyzer();
    IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    SortField indexSortField = new SortField("sortByNumber", SortField.Type.LONG);
    Sort indexSort = new Sort(indexSortField);;
    conf.setIndexSort(indexSort);


    conf.setUseCompoundFile(false);

    indexWriter = new IndexWriter(directory, conf);
    int count = 0;
    while (count++ < 1) {

      // 文档0
      Document doc = new Document();
      doc.add(new TextField("author", "aab b aab aabbcc ", Field.Store.YES));
      doc.add(new TextField("content", "a b", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 文档1
      doc = new Document();
      doc.add(new TextField("author", "cd a", Field.Store.YES));
      doc.add(new TextField("content", "a b c h", Field.Store.YES));
      doc.add(new TextField("title", "d a", Field.Store.YES));
      doc.add(new NumericDocValuesField("sortByNumber", -1));
      indexWriter.addDocument(doc);

      // 文档2
      doc = new Document();
      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
      doc.add(new TextField("content", "a c b e", Field.Store.YES));
      doc.add(new NumericDocValuesField("sortByNumber", 4));
      indexWriter.addDocument(doc);

      // 文档3
      doc = new Document();
      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
      doc.add(new TextField("content", "b c e", Field.Store.YES));
      doc.add(new NumericDocValuesField("sortByNumber", 1));
      indexWriter.addDocument(doc);

      // 文档4
      doc = new Document();
      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
      doc.add(new TextField("content", "a c e f g d", Field.Store.YES));
      indexWriter.addDocument(doc);

//      // 文档0
//      Document doc = new Document();
//      doc.add(new TextField("author", "aab b aab aabbcc ", Field.Store.YES));
//      doc.add(new TextField("content", "a b", Field.Store.YES));
//      indexWriter.addDocument(doc);
//
//      // 文档1
//      doc = new Document();
//      doc.add(new TextField("author", "cd a", Field.Store.YES));
//      doc.add(new TextField("content", "a b c h", Field.Store.YES));
//      doc.add(new TextField("title", "d a", Field.Store.YES));
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("a"))); // MIN
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("h"))); // MIDDLE_MAX
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("f"))); // MIDDLE_MIN
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("y"))); // MAX
//      indexWriter.addDocument(doc);
//
//      // 文档2
//      doc = new Document();
//      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
//      doc.add(new TextField("content", "a c b e", Field.Store.YES));
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("c"))); // MIN
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("i"))); // MIDDLE_MAX
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("e"))); // MIDDLE_MIN
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("z"))); // MAX
//      indexWriter.addDocument(doc);
//
//      // 文档3
//      doc = new Document();
//      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
//      doc.add(new TextField("content", "b c e", Field.Store.YES));
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("b"))); // MIN
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("j"))); // MIDDLE_MAX
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("d"))); // MIDDLE_MIN
//      doc.add(new SortedSetDocValuesField("sortByString", new BytesRef("x"))); // MAX
//      indexWriter.addDocument(doc);
//
//      // 文档4
//      doc = new Document();
//      doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
//      doc.add(new TextField("content", "b c e d f w e", Field.Store.YES));
//      indexWriter.addDocument(doc);
    }
    indexWriter.commit();
    // Per-top-reader state:

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "b")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "c")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "h")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "e")), BooleanClause.Occur.SHOULD);
    builder.setMinimumNumberShouldMatch(2);

    DirectoryReader reader = DirectoryReader.open(indexWriter);
    reader.maxDoc();
    IndexSearcher indexSearcher = new IndexSearcher(reader);
    SortField searchSortField = new SortField("sortByNumber", SortField.Type.LONG);
    Sort searchSort = new Sort(searchSortField);

    TopFieldCollector collector = TopFieldCollector.create(searchSort, 2, true, false, false, false);
    indexSearcher.search(new MatchAllDocsQuery(),  collector);

//    TopFieldDocs fieldDocs = indexSearcher.search(new MatchAllDocsQuery(), 5, searchSort);

    System.out.printf("ha");

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
