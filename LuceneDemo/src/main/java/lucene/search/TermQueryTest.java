package lucene.search;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-14 22:59
 */
public class TermQueryTest {

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

        Document doc ;
        // 0
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("a"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("b"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("c"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 3
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("a c e"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 4
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("h"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 5
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("c e"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 6
//        doc = new Document();
        doc.add(new StringField("content", new BytesRef("c a"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 7
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("f"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 8
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("b c d e c e"), Field.Store.YES));
        indexWriter.addDocument(doc);
        // 9
        doc = new Document();
        doc.add(new StringField("content", new BytesRef("a c e a b c"), Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "b")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "c")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "e")), BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(2);


        TopDocs docs = searcher.search(builder.build(), 3);

        for (ScoreDoc scoreDoc: docs.scoreDocs){
            Document document = searcher.doc(scoreDoc.doc);
            System.out.println("name is "+ document.get("abc")+"");
        }
    }

    public static void main(String[] args) throws Exception{
        TermQueryTest termQueryTest = new TermQueryTest();
        termQueryTest.doSearch();
    }
}
