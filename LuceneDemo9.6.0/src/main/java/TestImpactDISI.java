import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestImpactDISI {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void doSearch() throws Exception {
        IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(new WhitespaceAnalyzer()));
        addSegment(indexWriter, 10000);

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        Query query = new TermQuery(new Term("name", new BytesRef("abc")));
        IndexSearcher searcher = new IndexSearcher(reader);
        SortField sortField = new SortField("name", SortField.Type.SCORE);
        CollectorManager<TopFieldCollector, TopFieldDocs> manager =
                TopFieldCollector.createSharedManager(new Sort(sortField), 20, null, 20);
        TopDocs topDocs = searcher.search(query, manager);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            System.out.println(scoreDoc.doc);
            System.out.println(scoreDoc.score);
            System.out.println("------------------");
        }
        indexWriter.close();
        reader.close();
        directory.close();
        System.out.println("total hits: " + topDocs.totalHits.value);
        System.out.println("DONE");
    }

    void addSegment(IndexWriter indexWriter, int documentSize) throws Exception{
        Document doc;
        int count = 0;
        Random random = new Random();
        while (count++ != documentSize){
            doc = new Document();
            doc.add(new TextField("name", termValue(), Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
    }

    String termValue(){
       Random random = new Random();
       String value = "abc";
       int count = -1;
       while (count ++ < random.nextInt(6)){
           value = value + " abc";
       }
       String pengdingValue = " " + String.valueOf(random.nextInt(5));
       count = 0;
        while (count ++ < random.nextInt(10)){
            value = value + pengdingValue;
        }
      return value;
    }

    public static void main(String[] args) throws Exception {
        TestImpactDISI test = new TestImpactDISI();
        test.doSearch();
    }
}
