package index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndexSortSortedNumericDocValuesRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.FileOperation;

public class NumericDocValuesTopNOptimization2 {
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
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        int count = 0;

        Random random = new Random();
        boolean optimize = random.nextBoolean();
        System.out.println(optimize ? "enableOptimization" : "disableOptimization");

        Document doc;
        long sortValue;
        int shouldMatch = 0;
        while (count++ < 1000000) {
            if (count == 5 || count == 7 || count == 8) {
                sortValue = 10;
                shouldMatch++;
            } else if (count % 3 == 0) {
                sortValue = (long) random.nextInt(1000);
                if (sortValue >= 10) {
                    shouldMatch++;
                }
            } else {
                sortValue = random.nextLong();
                if (sortValue < 1000 && sortValue >= 10) {
                    shouldMatch++;
                }
            }
            doc = new Document();
            doc.add(new StringField("content", String.valueOf(sortValue), Field.Store.YES));
            doc.add(new NumericDocValuesField("sortField", sortValue));
            doc.add(new LongPoint("sortField", sortValue));
            indexWriter.addDocument(doc);
        }
        System.out.println("shouldMatch: " + shouldMatch);

        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        SortField sortField = new SortedNumericSortField("sortField", SortField.Type.LONG);
        long upperValue = 2000L;
        long lowerValue = 10L;
        Query fallbackQuery = LongPoint.newRangeQuery("sortField", lowerValue, upperValue);
        Query rangeQuery =
                new IndexSortSortedNumericDocValuesRangeQuery(
                        "sortField", lowerValue, upperValue, fallbackQuery);
        sortField.setCanUsePoints();
        Sort sort = new Sort(sortField);
        int topN = 3;
        TopFieldCollector collector = TopFieldCollector.create(sort, topN, 1000);
        searcher.search(rangeQuery, collector);
        System.out.println("收集器处理的文档数量: " + collector.getTotalHits() + "");
        for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
            System.out.println(
                    "文档号/文档值: " + scoreDoc.doc + " / " + searcher.doc(scoreDoc.doc).get("content"));
        }
    }

    public static void main(String[] args) throws Exception {
        NumericDocValuesTopNOptimization2 test = new NumericDocValuesTopNOptimization2();
        test.doSearch();
    }
}
