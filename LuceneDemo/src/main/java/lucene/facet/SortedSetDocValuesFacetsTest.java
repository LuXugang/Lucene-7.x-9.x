package lucene.facet;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.IOUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

/**
 * @author Lu Xugang
 * @date 2019-01-24 16:06
 */
public class SortedSetDocValuesFacetsTest {
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
    private IndexWriter writer;

    public void doFacet() throws Exception{
        FacetsConfig config = new FacetsConfig();
        config.setMultiValued("a", true);
        conf.setUseCompoundFile(false);
        writer = new IndexWriter(directory, conf);

        Document doc = new Document();
        doc.add(new SortedSetDocValuesFacetField("a", "foo"));
        doc.add(new SortedSetDocValuesFacetField("a", "bar"));
        doc.add(new SortedSetDocValuesFacetField("a", "zoo"));
        doc.add(new SortedSetDocValuesFacetField("b", "baz"));
        writer.addDocument(config.build(doc));

        doc = new Document();
        doc.add(new SortedSetDocValuesFacetField("a", "foo"));
        doc.add(new SortedSetDocValuesFacetField("b", "baz"));
        writer.addDocument(config.build(doc));

        writer.commit();

        IndexReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);
        // NRT open
        writer.close();

        // Per-top-reader state:
        SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader());

        Facets facets = getAllFacets(searcher, state);
        String result1 = facets.getTopChildren(10, "a").toString();
        String result2 = facets.getTopChildren(10, "b").toString();
        System.out.println("hah");
        System.out.println(facets.getTopChildren(10, "a").toString());
        System.out.println(facets.getTopChildren(10, "b").toString());
//        assertEquals("dim=a path=[] value=4 childCount=3\n  foo (2)\n  bar (1)\n  zoo (1)\n", facets.getTopChildren(10, "a").toString());
//        assertEquals("dim=b path=[] value=1 childCount=1\n  baz (1)\n", facets.getTopChildren(10, "b").toString());

        // DrillDown:
        DrillDownQuery q = new DrillDownQuery(config);
        q.add("a", "foo");
        q.add("b", "baz");
        TopDocs hits = searcher.search(q, 1);
//        assertEquals(1, hits.totalHits);

        writer.close();
    }

    private static Facets getAllFacets(IndexSearcher searcher, SortedSetDocValuesReaderState state) throws IOException{
        FacetsCollector c = new FacetsCollector();
        searcher.search(new MatchAllDocsQuery(), c);
        return new SortedSetDocValuesFacetCounts(state, c);
    }

    public static void main(String[] args) throws Exception{
        SortedSetDocValuesFacetsTest test = new SortedSetDocValuesFacetsTest();
        test.doFacet();
    }

}
