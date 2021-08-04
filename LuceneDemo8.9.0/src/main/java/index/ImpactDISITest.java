package index;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2021/7/9 2:55 下午
 */
public class ImpactDISITest {
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

//        List<Query> disjuncts = new ArrayList<>();
//
//        TermQuery titleTermQuery = new TermQuery(new Term("title", new BytesRef("china")));
//
//        TermQuery bodyTermQuery = new TermQuery(new Term("body", new BytesRef("china")));
//        BoostQuery bodyBoostQuery = new BoostQuery(bodyTermQuery, 5);
//
//        disjuncts.add(titleTermQuery);
//        disjuncts.add(bodyBoostQuery);
//
//        DisjunctionMaxQuery disjunctionMaxQuery = new DisjunctionMaxQuery(disjuncts, 0);


        TermQuery titleTermQuery = new TermQuery(new Term("title", new BytesRef("china")));

        TermQuery bodyTermQuery = new TermQuery(new Term("body", new BytesRef("china")));
        BoostQuery bodyBoostQuery = new BoostQuery(bodyTermQuery, 5);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(titleTermQuery, BooleanClause.Occur.SHOULD);
        builder.add(bodyBoostQuery, BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(1);

        BooleanQuery booleanQuery = builder.build();


    }
    public static void main(String[] args) throws Exception{
        ImpactDISITest test = new ImpactDISITest();
        test.doSearch();
    }
}
