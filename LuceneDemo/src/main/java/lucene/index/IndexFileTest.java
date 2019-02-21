package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

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
  private IndexWriter writer;

  public void doIndex() throws Exception {
    conf.setUseCompoundFile(false);
    writer = new IndexWriter(directory, conf);

    // 1
    Document doc = new Document();
    doc.add(new TextField("content", "this a this liucixin a fiction", Field.Store.YES));
    doc.add(new TextField("author", "liucixin", Field.Store.YES));
    doc.add(new TextField("title", "wandering", Field.Store.YES));
    doc.add(new SortedDocValuesField("docvale", new BytesRef("hha")));
    writer.addDocument(doc);

    // 2
    doc = new Document();
    doc.add(new TextField("content", "this a zhang fiction bright", Field.Store.YES));
    doc.add(new TextField("author", "zhang", Field.Store.YES));
    doc.add(new TextField("title", "mars", Field.Store.YES));
    writer.addDocument(doc);

    // 3
    doc = new Document();
    doc.add(new TextField("content", "this a liudehua fiction small", Field.Store.YES));
    doc.add(new TextField("author", "liudehua", Field.Store.YES));
    doc.add(new TextField("title", "urik", Field.Store.YES));
    writer.addDocument(doc);

    // 4
    doc = new Document();
    doc.add(new TextField("content", "this a tanhaoqiang fiction honor", Field.Store.YES));
    doc.add(new TextField("author", "tanhaoqiang", Field.Store.YES));
    doc.add(new TextField("title", "c++", Field.Store.YES));
    writer.addDocument(doc);

    writer.commit();

    // Per-top-reader state:
  }

  public static void main(String[] args) throws Exception{
    IndexFileTest test = new IndexFileTest();
    test.doIndex();
  }
}
