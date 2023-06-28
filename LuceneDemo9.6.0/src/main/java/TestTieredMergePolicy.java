import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

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
//        conf.setMergePolicy(NoMergePolicy.INSTANCE);
        Random random = new Random();
        Document doc;
        //
        int segmentSize = 1;
        int segmentCount = 0;
        int count = 0;
        String stringValue;
        while (count++ != 5000){
            stringValue = String.valueOf(random.nextInt(1000000));
            if(count == 3){
                doc = new Document();
                doc.add(new StringField("content", stringValue, StringField.Store.YES));
                indexWriter.addDocument(doc);
            }else if(count == 2010 || count == 2011){
                doc = new Document();
                doc.add(new StringField("content", stringValue, StringField.Store.YES));
                indexWriter.addDocument(doc);
            }else {
                doc = new Document();
                doc.add(new StringField("age", String.valueOf(count), StringField.Store.YES));
                doc.add(new SortedDocValuesField("name", new BytesRef(stringValue)));
                doc.add(new StringField("name", stringValue, StringField.Store.YES));
                indexWriter.addDocument(doc);
            }
        }
        indexWriter.commit();

//        indexWriter.forceMerge(1);
//
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new MatchAllDocsQuery();
        Sort sort = new Sort(new SortedSetSortField("name", true));
        TopFieldDocs docs  = searcher.search(query, 2000, sort);
        System.out.println(docs.totalHits.value);


        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TestTieredMergePolicy test = new TestTieredMergePolicy();
        test.doSearch();
    }
}
