package lucene.search;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
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
public class TermQuerySHOULDTest {

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
        doc.add(new TextField("content", "a", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new TextField("content", "b", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new TextField("content", "c", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 3
        doc = new Document();
        doc.add(new TextField("content", "a c e", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 4
        doc = new Document();
        doc.add(new TextField("content", "h", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 5
        doc = new Document();
        doc.add(new TextField("content", "c e", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 6
        doc = new Document();
        doc.add(new TextField("content", "c a", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 7
        doc = new Document();
        doc.add(new TextField("content", "f", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 8
        doc = new Document();
        doc.add(new TextField("content", "b c d e c e", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 9
        doc = new Document();
        doc.add(new TextField("content", "a c e a b c", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "b")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "c")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "e")), BooleanClause.Occur.MUST_NOT);
        builder.setMinimumNumberShouldMatch(2);


        ScoreDoc []docs = searcher.search(builder.build(), 10).scoreDocs;

        System.out.println("hah");
    }

    public static void main(String[] args) throws Exception{
        TermQuerySHOULDTest termQuerySHOULDTest = new TermQuerySHOULDTest();
        termQuerySHOULDTest.doSearch();
    }
}
