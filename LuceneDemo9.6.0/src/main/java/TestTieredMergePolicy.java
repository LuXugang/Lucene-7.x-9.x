import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class TestTieredMergePolicy {
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
        indexWriter = new IndexWriter(directory, conf);
        conf.setMergeScheduler(new SerialMergeScheduler());
        Document doc;
        int fixedValue = 3;
        //
        int count = 0;
        while (count++ != 100000){
                doc = new Document();
                doc.add(new NumericDocValuesField("num", count));
                doc.add(new IntPoint("num", count));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();

        indexWriter.forceMerge(1);
//
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        SortField sortField = new SortField("abc", SortField.Type.STRING);
        Sort sort = new Sort(sortField);

        CollectorManager<TopFieldCollector, TopFieldDocs> manager =
                TopFieldCollector.createSharedManager(sort, 2000, null, 2000);
        TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), manager);

        System.out.println(topDocs.totalHits.value);


        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TestTieredMergePolicy test = new TestTieredMergePolicy();
        test.doSearch();
    }
}
