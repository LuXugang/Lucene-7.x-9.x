package facet;

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
import java.util.Random;

public class IndexOrDocValueQuery {
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
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new NumericDocValuesField("a", 1));
        doc.add(new NumericDocValuesField("b", 2));
        doc.add(new NumericDocValuesField("c", 3));
        doc.add(new SortedNumericDocValuesField("a-b-c", 1));
        doc.add(new SortedNumericDocValuesField("a-b-c", 2));
        doc.add(new SortedNumericDocValuesField("a-b-c", 3));
        doc.add(new StringField("termFiled", "my", Field.Store.YES));
        doc.add(new IntPoint("number", 2));
        doc.add(new SortedNumericDocValuesField("number", 2));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new IntPoint("number", 2));
        doc.add(new SortedNumericDocValuesField("number", 2));
        doc.add(new StringField("termFiled", "my", Field.Store.YES));
        indexWriter.addDocument(doc);
        int count = 0 ;
        int a;
        while (count++ < 40960){
            doc = new Document();
            a = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            doc.add(new IntPoint("number", a));
            doc.add(new SortedNumericDocValuesField("number", a));
            doc.add(new StringField("content", "ddf", Field.Store.YES));
            indexWriter.addDocument(doc);
            if(count % 2000 == 0)
                indexWriter.commit();
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        int lowValue = Integer.MIN_VALUE;
        int upValue = Integer.MAX_VALUE;

        Query termQuery = new TermQuery(new Term("termFiled", new BytesRef("my")));

        Query pointsRangeQuery = IntPoint.newRangeQuery("number", -100, 999999);
        Query docValuesRangeQuery = SortedNumericDocValuesField.newSlowRangeQuery("number", -100, 999999);
        Query indexOrDocValuesQuery = new IndexOrDocValuesQuery(pointsRangeQuery, docValuesRangeQuery);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(termQuery, BooleanClause.Occur.MUST);
        builder.add(indexOrDocValuesQuery, BooleanClause.Occur.MUST);
        Query query = builder.build();

        // 返回Top5的结果
        int resultTopN = 10000;

        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档号: "+scoreDoc.doc+"");
        }

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        IndexOrDocValueQuery test = new IndexOrDocValueQuery();
        test.doSearch();
    }
}
