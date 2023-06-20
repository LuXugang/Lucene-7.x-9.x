import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
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
        int segmentSize = 11;
        int segmentCount = 0;
        int count = 0;
        while (segmentCount++ < segmentSize){
            count = 0;
            while (count++ != 310000){
                doc = new Document();
                doc.add(new SortedSetDocValuesField("sortedSet", new BytesRef("a")));
                doc.add(new StringField("name", String.valueOf(random.nextInt(1000000)), StringField.Store.YES));
                indexWriter.addDocument(doc);
            }
            indexWriter.commit();
        }

//        indexWriter.forceMerge(1);
//
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new MatchAllDocsQuery();
        System.out.println("段的数量: "+reader.leaves().size());
        for (LeafReaderContext leaf : reader.leaves()) {
            System.out.println("段: "+leaf.toString()+"");
        }
        System.out.println(reader.maxDoc());
        ScoreDoc[] scoreDocs = searcher.search(query, 2000000).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {

        }

        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TestTieredMergePolicy test = new TestTieredMergePolicy();
        test.doSearch();
    }
}
