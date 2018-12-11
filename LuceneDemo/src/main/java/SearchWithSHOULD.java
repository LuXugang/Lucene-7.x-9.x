import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * @author Lu Xugang
 * @date 2018-12-04 20:49
 */
public class SearchWithSHOULD {
        private Directory directory = new RAMDirectory();
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter writer;

    private void doSearch() throws Exception{
        writer = new IndexWriter(directory, conf);

        String[] docs = {
                "a",
                "b",
                "c",
                "a c e",
                "c a",
                "b c d e c e",
                "a c e a b c"
        };

        for (String s: docs
        ) {
            Document document = new Document();
            document.add(new TextField("content", s, Field.Store.YES));
            writer.addDocument(document);
        }
        writer.commit();
        IndexReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
        query.add(new TermQuery(new Term("content", "c")), BooleanClause.Occur.SHOULD);
        query.add(new TermQuery(new Term("content", "e")), BooleanClause.Occur.MUST);
        query.setMinimumNumberShouldMatch(2);

        ScoreDoc[] hits;
        hits = searcher.search(query.build(), 1000).scoreDocs;

        System.out.println("hit size: "+ hits.length +"");
        for (int i = 0 ; i < hits.length && i < 10; i++) {
            Document d = searcher.doc(hits[i].doc);
            System.out.println(i + " " + hits[i].score + " " + d.get("content"));
        }
        reader.close();
        directory.close();
    }

    public static void main(String[] args) throws Exception{
        SearchWithSHOULD simpleSearch = new SearchWithSHOULD();
        simpleSearch.doSearch();
    }
}
