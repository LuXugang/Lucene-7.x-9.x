package util.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2021/6/15 2:20 下午
 */
public class BlockMaxConjunctionScorerTest {

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
        while (count++ < 6){
            doc = new Document();
            doc.add(new TextField("author",  "andy lily baby andy lucy ably", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            if(count == 130){
                doc.add(new TextField("author",  "lily andy tom lucy for you", Field.Store.YES));
            }else {
                doc.add(new TextField("author",  "lily andy tom lucy for you", Field.Store.YES));
            }
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("author", "lily")), BooleanClause.Occur.MUST);
        builder.add(new TermQuery(new Term("author", "lucy")), BooleanClause.Occur.MUST);
        ScoreDoc[] scoreDocs = searcher.search(builder.build(), 100).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.print(scoreDoc.doc);
            System.out.print(" ");
        }

        System.out.println("DONE");

    }

    public static void main(String[] args) throws Exception{
        BlockMaxConjunctionScorerTest test = new BlockMaxConjunctionScorerTest();
        test.doSearch();
    }
}
