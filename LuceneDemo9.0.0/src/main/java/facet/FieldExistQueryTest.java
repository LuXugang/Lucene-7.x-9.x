package facet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NormsFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class FieldExistQueryTest {
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

        int count = 0 ;
        int a;
        while (count++ < 1000000){
            doc = new Document();
            a = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            doc.add(new LongPoint("number", a));
            doc.add(new TextField("number", "abc", Field.Store.YES));
            doc.add(new SortedNumericDocValuesField("number", a));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        for (int i = 0; i < 100; i++) {
            Query query1 = new NormsFieldExistsQuery("number");
            long start = System.currentTimeMillis();
            Collector collector = new TotalHitCountCollector();
            searcher.search(query1, collector);
            long end = System.currentTimeMillis();
            System.out.println(end - start);

            start = System.currentTimeMillis();
            collector = new TotalHitCountCollector();
            Query query2 = new DocValuesFieldExistsQuery("number");
            searcher.search(query2, collector);
            end = System.currentTimeMillis();
            System.out.println(end - start);
            System.out.println("-----------------------");

        }

        // 返回Top5的结果
        int resultTopN = 10000000;


        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        FieldExistQueryTest test = new FieldExistQueryTest();
        test.doSearch();
    }
}
