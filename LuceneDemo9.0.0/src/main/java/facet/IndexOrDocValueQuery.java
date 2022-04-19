package facet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.sandbox.search.IndexSortSortedNumericDocValuesRangeQuery;
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
        SortField sortField = new SortedNumericSortField("number2", SortField.Type.LONG);
        sortField.setMissingValue(1L);
        Sort indexSort = new Sort(sortField);
//        Sort indexSort = new Sort(new SortedNumericSortField("number", SortField.Type.LONG, true, SortedNumericSelector.Type.MAX));
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        conf.setIndexSort(indexSort);
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new StringField("termFiled", "my", Field.Store.YES));
        doc.add(new StringField("number1", "my", Field.Store.YES));
        doc.add(new LongPoint("number", 5));
        doc.add(new SortedNumericDocValuesField("number", 5));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new LongPoint("number", 2));
        doc.add(new SortedNumericDocValuesField("number", 2));
        doc.add(new StringField("termFiled", "my", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 文档2
        doc = new Document();
        doc.add(new LongPoint("number", -1));
        doc.add(new SortedNumericDocValuesField("number", -1));
        doc.add(new StringField("termFiled", "my", Field.Store.YES));
        indexWriter.addDocument(doc);
        int count = 0 ;
        int a;
//        while (count++ < 4096){
//            doc = new Document();
//            a = random.nextInt(100);
//            a = a <= 2 ? a + 4 : a;
//            doc.add(new LongPoint("number", a));
//            doc.add(new SortedNumericDocValuesField("number", a));
//            if(count % 17 != 0){
//                doc.add(new StringField("termFiled", "my", Field.Store.YES));
//            }
//            indexWriter.addDocument(doc);
//        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        int lowValue = Integer.MIN_VALUE;
        int upValue = Integer.MAX_VALUE;

        Query termQuery = new TermQuery(new Term("number", new BytesRef("my")));

        Query pointsRangeQuery = LongPoint.newRangeQuery("termFiled", -100, 80);
        Query docValuesRangeQuery = SortedNumericDocValuesField.newSlowRangeQuery("number1", -100, 80);
        Query indexOrDocValuesQuery = new IndexOrDocValuesQuery(pointsRangeQuery, docValuesRangeQuery);

        Query sortSortQuery = new IndexSortSortedNumericDocValuesRangeQuery("number1", -100, 80, docValuesRangeQuery);

        Query docValueExist= new DocValuesFieldExistsQuery("number");

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(termQuery, BooleanClause.Occur.MUST);
        builder.add(sortSortQuery, BooleanClause.Occur.MUST);
//        builder.add(indexOrDocValuesQuery, BooleanClause.Occur.MUST);
        Query query = builder.build();


        Weight weight = docValueExist.createWeight(searcher, ScoreMode.COMPLETE, 1.0f);
        for (LeafReaderContext context : searcher.getIndexReader().leaves()) {
             weight.count(context);
        }

        // 返回Top5的结果
        int resultTopN = 10000000;

        ScoreDoc[] scoreDocs = searcher.search(docValueExist, resultTopN).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档号: "+scoreDoc.doc+"");
        }
        System.out.println("匹配的文档数量: "+scoreDocs.length+"");

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        IndexOrDocValueQuery test = new IndexOrDocValueQuery();
        test.doSearch();
    }
}
