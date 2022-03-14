package facet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.sandbox.search.IndexSortSortedNumericDocValuesRangeQuery;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 文章中的例子，请勿改动
 */
public class MissingValueTest {
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
        SortField sortField = new SortedNumericSortField("number", SortField.Type.LONG, false);
        sortField.setMissingValue(3L);
        Sort indexSort = new Sort(sortField);
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        conf.setIndexSort(indexSort);
        indexWriter = new IndexWriter(directory, conf);
        Document doc;

        // 文档0
        doc = new Document();
        doc.add(new StringField("content", "my", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 文档1
        doc = new Document();
        doc.add(new LongPoint("number", 5));
        doc.add(new SortedNumericDocValuesField("number", 5));
        indexWriter.addDocument(doc);

        // 文档2
        doc = new Document();
        doc.add(new LongPoint("number", 10));
        doc.add(new SortedNumericDocValuesField("number", 10));
        indexWriter.addDocument(doc);

        indexWriter.commit();
        int lowerValue = 1;
        int upperValue = 100;

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query pointsRangeQuery = LongPoint.newRangeQuery("number", lowerValue, upperValue);
        Query docValuesRangeQuery = SortedNumericDocValuesField.newSlowRangeQuery("number", lowerValue, upperValue);
        Query indexOrDocValuesQuery = new IndexOrDocValuesQuery(pointsRangeQuery, docValuesRangeQuery);

        Query sortSortQuery = new IndexSortSortedNumericDocValuesRangeQuery("number", lowerValue, upperValue, indexOrDocValuesQuery);

        ScoreDoc[] scoreDocs = searcher.search(sortSortQuery, 100).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档号: "+scoreDoc.doc+"");
        }
        System.out.println("匹配的文档数量: "+scoreDocs.length+"");

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        MissingValueTest test = new MissingValueTest();
        test.doSearch();
    }
}
