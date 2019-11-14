package lucene.query.FunctionScoerQuery;

import io.FileOperation;
import lucene.query.TermQuerySHOULDMUSTTest;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class FunctionScoreQueryTest {

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

        int count = 0;
//        while (count++ < 7999) {
            doc = new Document();
            doc.add(new TextField("content", "a e c", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 1
            doc = new Document();
            doc.add(new TextField("content", "e", Field.Store.YES));
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
            doc.add(new TextField("content", "b c", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 6
            doc = new Document();
            doc.add(new TextField("content", "c a", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 7
            doc = new Document();
            doc.add(new TextField("content", "a e h", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 8
            doc = new Document();
            doc.add(new TextField("content", "b c d e h e", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 9
            doc = new Document();
            doc.add(new TextField("content", "a e a b ", Field.Store.YES));
            indexWriter.addDocument(doc);
//        }
        indexWriter.commit();

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query1 = FunctionScoreQuery.boostByValue(new TermQuery(new Term("content", "a")), DoubleValuesSource.constant(2));
        Query query2 = FunctionScoreQuery.boostByValue(new TermQuery(new Term("content", "b")), DoubleValuesSource.constant(3));
        Query query3 = FunctionScoreQuery.boostByValue(new TermQuery(new Term("content", "c")), DoubleValuesSource.constant(4));
//        Query query1 = new TermQuery(new Term("content", "a"));
//        Query query2 = new TermQuery(new Term("content", "b"));
//        Query query3 = new TermQuery(new Term("content", "c"));
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(query1, BooleanClause.Occur.SHOULD);
        builder.add(query2, BooleanClause.Occur.SHOULD);
        builder.add(query3, BooleanClause.Occur.SHOULD);

        TotalHitCountCollector collector = new TotalHitCountCollector();

        ScoreDoc[] docs = searcher.search(builder.build(), 20).scoreDocs;
        for (int i = 0; i < docs.length; i++) {
            System.out.println("docId: "+docs[i].doc+", score: "+docs[i].score+"");
        }

        System.out.println("hah");
    }

    public static void main(String[] args) throws Exception{
        FunctionScoreQueryTest test = new FunctionScoreQueryTest();
        test.doSearch();
    }
}
