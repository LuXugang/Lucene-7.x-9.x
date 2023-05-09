import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestBooleanQuery {
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

    public void doSearch() throws Exception {
        SortField field = new SortedSetSortField("name", false);
        field.setMissingValue(SortField.STRING_LAST);
        Sort indexSearch = new Sort(field);
        conf.setUseCompoundFile(false);
//        conf.setIndexSort(indexSearch);
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        String aaa = null;
        String bbb = null;

        // 文档0
        doc = new Document();
        doc.add(new StringField("name", "a", Field.Store.YES));
        doc.add(new StringField("name", "z", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 文档1
        doc = new Document();
        doc.add(new StringField("name", "b", Field.Store.YES));
        doc.add(new StringField("name", "z", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 文档2
        doc = new Document();
        doc.add(new StringField("name", "c", Field.Store.YES));
        doc.add(new StringField("name", "z", Field.Store.YES));
        indexWriter.addDocument(doc);


        // 通过构建器模式创建BooleanQuery对象
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Query termQuery = new MatchAllDocsQuery();
        BooleanClause clause = new BooleanClause(termQuery, BooleanClause.Occur.FILTER);
        builder.add(clause);

        builder.add(new TermQuery(new Term("name", new BytesRef("a"))), BooleanClause.Occur.SHOULD);
//        builder.add(new TermQuery(new Term("name", new BytesRef("z"))), BooleanClause.Occur.MUST);
//        builder.add(new TermQuery(new Term("name", new BytesRef("z"))), BooleanClause.Occur.MUST);


        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = builder.build();
        // 返回Top5的结果
        int resultTopN = 1000;

        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN).scoreDocs;
//        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN).scoreDocs;

        System.out.println("Total Result Number: "+scoreDocs.length+"");
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            // 输出满足查询条件的 文档号
            System.out.println(""+i+"：doc id: "+scoreDoc.doc+": 文档"+reader.document(scoreDoc.doc).get("name")+"");
        }
    }

    public static void main(String[] args) throws Exception{
        TestBooleanQuery test = new TestBooleanQuery();
        test.doSearch();
    }
}
