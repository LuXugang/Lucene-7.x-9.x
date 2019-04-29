package lucene.index;

import io.FileOperation;
import lucene.AnalyzerTest.PayloadAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-26 10:06
 */
public class TermVectorTest {

  private Directory directory;

  {
    try {
      FileOperation.deleteFile("./data");
      directory = new MMapDirectory(Paths.get("./data"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  IndexWriter indexWriter;


  public void doSearch() throws Exception {
    PayloadAnalyzer analyzer = new PayloadAnalyzer();
    IndexWriterConfig conf = new IndexWriterConfig(analyzer);

    conf.setUseCompoundFile(false);
    indexWriter = new IndexWriter(directory, conf);

    FieldType type = new FieldType();
    type.setStored(true);
    type.setStoreTermVectors(true);
    type.setStoreTermVectorPositions(true);
    type.setStoreTermVectorPayloads(true);
    type.setStoreTermVectorOffsets(true);
    type.setTokenized(true);
    type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);


    analyzer.setPayloadData("content", "it is payload".getBytes(StandardCharsets.UTF_8), 0, 13);
    Document doc ;
    // 0
    doc = new Document();
    doc.add(new Field("content", "the book is book", type));
    doc.add(new Field("title", "book", type));
    indexWriter.addDocument(doc);
    // 1
    doc = new Document();
    doc.add(new Field("content", "the fake news is news", type));
    doc.add(new Field("title", "news", type));
    indexWriter.addDocument(doc);
    // 2
    doc = new Document();
    doc.add(new Field("content", "the name is name", type));
    indexWriter.addDocument(doc);
    indexWriter.commit();


    IndexReader reader = DirectoryReader.open(indexWriter);
    IndexSearcher searcher = new IndexSearcher(reader);

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "b")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "c")), BooleanClause.Occur.SHOULD);
    builder.add(new TermQuery(new Term("content", "h")), BooleanClause.Occur.SHOULD);
    builder.setMinimumNumberShouldMatch(2);


    ScoreDoc[]docs = searcher.search(builder.build(), 10).scoreDocs;

    System.out.println("hah");
  }


  public static void main(String[] args) throws Exception{
    TermVectorTest termVectorTest = new TermVectorTest();
    termVectorTest.doSearch();
  }
}
