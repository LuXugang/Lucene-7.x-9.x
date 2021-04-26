package util.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;


/**
 * @author Lu Xugang
 * @date 2021/4/19 5:01 下午
 */
public class MemoryIndexTest {

    private final Analyzer analyzer = new WhitespaceAnalyzer();
    private final IndexWriterConfig conf = new IndexWriterConfig(analyzer);

    public void doSearch() throws Exception {
        Document document = new Document();
        document.add(new StringField("author", "jay", Field.Store.YES));
        document.add(new StringField("author", "lily", Field.Store.YES));
        document.add(new StringField("author", "jay", Field.Store.YES));
        document.add(new StringField("author", "lucy", Field.Store.YES));
        int count = 0;
        MemoryIndex memoryIndex = MemoryIndex.fromDocument(document, analyzer, true, true, 0);
        Query query = new TermQuery(new Term("author", new BytesRef("lily")));
        float score = memoryIndex.search(query);
        if (score >= 0.0f) {
            System.out.println("it's a match: "+score+"");
            System.out.println(memoryIndex.toStringDebug());
        } else {
            System.out.println("no match found");
        }
    }

    public static void main(String[] args) throws Exception{
        MemoryIndexTest test = new MemoryIndexTest();
        test.doSearch();
    }
}
