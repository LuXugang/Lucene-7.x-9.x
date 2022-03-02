package facet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.StringDocValuesReaderState;
import org.apache.lucene.facet.StringValueFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class StringValueFacetTest {
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
    private IndexWriter writer;

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        writer = new IndexWriter(directory, conf);

        Document doc = new Document();
        doc.add(new StringField("abc", "abc", Field.Store.YES));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new SortedSetDocValuesField("field", new BytesRef("foo")));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new SortedSetDocValuesField("field", new BytesRef("bar")));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new SortedSetDocValuesField("field", new BytesRef("bar")));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new SortedSetDocValuesField("field", new BytesRef("baz")));
        writer.addDocument(doc);

        DirectoryReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);

        StringDocValuesReaderState state =
                new StringDocValuesReaderState(searcher.getIndexReader(), "field");

        FacetsCollector c = new FacetsCollector();
        searcher.search(new MatchAllDocsQuery(), c);

        StringValueFacetCounts facets = new StringValueFacetCounts(state, c);


        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        StringValueFacetTest test = new StringValueFacetTest();
        test.doSearch();
    }
}
