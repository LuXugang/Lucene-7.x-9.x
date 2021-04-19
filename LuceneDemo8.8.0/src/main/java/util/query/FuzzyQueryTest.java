package util.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.FileOperation;
import util.index.WANDScorerTest;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2021/4/19 2:34 下午
 */
public class FuzzyQueryTest {

    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Analyzer analyzer = new WhitespaceAnalyzer();
    private final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        int count = 0;
        Document doc ;
        while (count++ < 1){
            doc = new Document();
            doc.add(new TextField("author",  "llou lily baby andy lucy ably", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new TextField("author",  "lily andy tom good for yyou", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new TextField("author",  "llyou", Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query fuzzyQuery = new FuzzyQuery(new Term("author", "uo"), 2, 0 , 5, false);
        ScoreDoc[] scoreDocs = searcher.search(fuzzyQuery, 100).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.print(scoreDoc.doc);
            System.out.print(" ");
        }
    }

    public static void main(String[] args) throws Exception{
        FuzzyQueryTest test = new FuzzyQueryTest();
        test.doSearch();
    }
}
