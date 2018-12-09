import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lu Xugang
 * @date 2018-12-05 09:55
 */
public class SearchWithMemoryIndex {
    static MemoryIndex index = new MemoryIndex(true, true);

    private static void start() throws Exception{
        Analyzer analyzer = new WhitespaceAnalyzer();
        Map<String, String> event = new HashMap<String, String>();
        event.put("content", "Readings about Salmons and other select Alaska fishing Manuals");
        event.put("author", "Tales of Tales James");

        for(Map.Entry<String, String> entry : event.entrySet()){
            index.addField(entry.getKey(), entry.getValue(),analyzer);
        }

        Query query1 = new TermQuery(new Term("author", "Tales"));
        Query query2 = new TermQuery(new Term("content", "other"));
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(query1, BooleanClause.Occur.SHOULD);
        builder.add(query2, BooleanClause.Occur.SHOULD);

        float score = index.search(builder.build());

        if (score > 0.0f) {
            System.out.println("it's a match");
        } else {
            System.out.println("no match found");
        }
        System.out.println("indexData=" + index.toStringDebug());
//
    }

    public static void main(String[] args) throws Exception{
        start();
    }

}
