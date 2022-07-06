package facet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.NormsFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestKnnTieBreak {
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

        for (int j = 0; j < 5; j++) {
            doc = new Document();
            doc.add(new KnnVectorField("field", new float[] {0, 1}, VectorSimilarityFunction.EUCLIDEAN));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        try (IndexReader reader = DirectoryReader.open(indexWriter)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            KnnVectorQuery query = new KnnVectorQuery("field", new float[] {2, 3}, 3);
            ScoreDoc[] scoreDocs = searcher.search(query, 3).scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                System.out.println("docId: "+scoreDoc.doc+"");
            }
        }
    }
    public static void main(String[] args) throws Exception{
        TestKnnTieBreak test = new TestKnnTieBreak();
        test.doSearch();
    }
}
