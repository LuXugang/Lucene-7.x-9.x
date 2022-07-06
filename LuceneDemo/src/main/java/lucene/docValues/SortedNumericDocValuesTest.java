package lucene.docValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-10 10:54
 */
public class SortedNumericDocValuesTest {
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

  public void doSearch() throws Exception {
    conf.setUseCompoundFile(false);
    indexWriter = new IndexWriter(directory, conf);

    String groupField1 = "age";
    String groupField2 = "price";
    // 0
    int count = 0;
    while (count++ < 1) {
      Document doc = new Document();
      doc.add(new SortedNumericDocValuesField(groupField1, 92L));
      doc.add(new SortedNumericDocValuesField(groupField2, 100L));
      doc.add(new StringField("name", "luxugang", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 1
      doc = new Document();
      doc.add(new SortedNumericDocValuesField("age", 92));
      doc.add(new SortedNumericDocValuesField("age", 93));
      indexWriter.addDocument(doc);

      // 2
      doc = new Document();
      doc.add(new SortedNumericDocValuesField(groupField1, 92L));
        doc.add(new SortedNumericDocValuesField(groupField1, 912L));
      doc.add(new SortedNumericDocValuesField(groupField2, 100L));
      doc.add(new StringField("name", "liudehua", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 3
      doc = new Document();
      doc.add(new SortedNumericDocValuesField(groupField1, 24L));
      doc.add(new StringField("name", "guofucheng", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 4
      doc = new Document();
      doc.add(new SortedNumericDocValuesField(groupField1, 20L));
      doc.add(new StringField("name", "zhengxueyou", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 5
      doc = new Document();
      doc.add(new SortedNumericDocValuesField(groupField1, 42L));
      doc.add(new StringField("name", "luxugang", Field.Store.YES));
      indexWriter.addDocument(doc);

      // 6
      doc = new Document();
      doc.add(new SortedNumericDocValuesField(groupField1, 42L));
      doc.add(new StringField("name", "zhengyijian", Field.Store.YES));
      indexWriter.addDocument(doc);


    }

    indexWriter.commit();

    IndexReader reader = DirectoryReader.open(indexWriter);
    IndexSearcher searcher = new IndexSearcher(reader);


//    Sort sort = new Sort(new SortedNumericSortField("age", SortField.Type.LONG), new SortedNumericSortField("price", SortField.Type.LONG));
    Sort sort = new Sort(new SortedNumericSortField("age", SortField.Type.LONG));

    TopDocs docs = searcher.search(new MatchAllDocsQuery(), 10000, sort);

   for (ScoreDoc scoreDoc: docs.scoreDocs){
     Document document = searcher.doc(scoreDoc.doc);
       System.out.println("id: "+scoreDoc.doc+" score: "+scoreDoc.score+"");
   }

  }

  public static void main(String[] args) throws Exception{
    SortedNumericDocValuesTest test = new SortedNumericDocValuesTest();
    test.doSearch();
  }
}
