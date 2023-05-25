import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestDocValuesRangeQuery {
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
        conf.setUseCompoundFile(false);
//        conf.setIndexSort(indexSearch);
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc = new Document();
        // 文档0

        doc.add(new SortedNumericDocValuesField("name", 2));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new SortedNumericDocValuesField("name", 3));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new SortedNumericDocValuesField("name", 4));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new StringField("abc", "df", Field.Store.YES));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new SortedNumericDocValuesField("name", 5));
        indexWriter.addDocument(doc);

        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        System.out.println("document count : "+reader.maxDoc()+"");


        Query query = SortedNumericDocValuesField.newSlowRangeQuery("name", 3, 9);
        SortField searchSortField = new SortedSetSortField("name", false);
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
        TestDocValuesRangeQuery test = new TestDocValuesRangeQuery();
        test.doSearch();
    }
}
