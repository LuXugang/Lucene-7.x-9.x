import java.io.File;
import java.nio.file.Paths;
import java.util.Random;
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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

/** example for https://github.com/apache/lucene/issues/11773 , do not modifier the code */
public class NumericDocValuesTopNOptimization2 {

    public void doSearch() throws Exception {
        Directory directory;
        deleteFile("./data");
        directory = new MMapDirectory(Paths.get("./data"));
        IndexWriterConfig conf = new IndexWriterConfig(new WhitespaceAnalyzer());
        conf.setUseCompoundFile(true);
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        int count = 0;

        Random random = new Random();
        long upperValue = Long.MAX_VALUE;
        long lowerValue = 10L;
        long sortValue;
        // add top3 documents
        indexWriter.addDocument(addDocument(lowerValue, "sortedField"));
        indexWriter.addDocument(addDocument(lowerValue, "sortedField"));
        indexWriter.addDocument(addDocument(lowerValue, "sortedField"));
        // index 80000 documents in boundary should be matched by PointRangeQuery
        while (count++ < 80000) {
            sortValue = random.nextLong();
            if (sortValue <= lowerValue) {
                // make sure the sortValue is in the query boundary
                sortValue = 20L;
            }
            indexWriter.addDocument(addDocument(sortValue, "sortedField"));
        }

        // index documents which are out of boundary
        int outOfBoundaryDocumentCount = random.nextBoolean() ? 10 : 20000;
        outOfBoundaryDocumentCount = 20000;
        System.out.println("outOfBoundaryDocumentCount:" + outOfBoundaryDocumentCount);
        count = 0;
        while (count++ < outOfBoundaryDocumentCount) {
            sortValue = random.nextInt((int) lowerValue - 1);
            indexWriter.addDocument(addDocument(sortValue, "sortedField"));
        }

        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        SortField sortField = new SortedNumericSortField("sortedField", SortField.Type.LONG);
        sortField.setMissingValue(Long.MAX_VALUE);
        Query rangeQuery = LongPoint.newRangeQuery("sortedField", lowerValue, upperValue);
        Sort sort = new Sort(sortField);
        int topN = 3;
        TopFieldCollector collector = TopFieldCollector.create(sort, topN, 1000);
        searcher.search(rangeQuery, collector);
        System.out.println("hits in collector: " + collector.getTotalHits() + "");
        indexWriter.close();
        ;
        reader.close();
    }

    private Document addDocument(Long value, String fieldName) {
        Document doc = new Document();
        doc.add(new StringField("content", String.valueOf(value), Field.Store.YES));
        doc.add(new NumericDocValuesField(fieldName, value));
        doc.add(new LongPoint(fieldName, value));
        return doc;
    }

    public static void main(String[] args) throws Exception {
        NumericDocValuesTopNOptimization2 test = new NumericDocValuesTopNOptimization2();
        int count = 5;
        for (int i = 0; i < count; i++) {
            test.doSearch();
        }
    }

    public static void deleteFile(String filePath) {
        File dir = new File(filePath);
        if (dir.exists()) {
            File[] tmp = dir.listFiles();
            assert tmp != null;
            for (File aTmp : tmp) {
                if (aTmp.isDirectory()) {
                    deleteFile(filePath + "/" + aTmp.getName());
                } else {
                    aTmp.delete();
                }
            }
            dir.delete();
        }
    }
}
