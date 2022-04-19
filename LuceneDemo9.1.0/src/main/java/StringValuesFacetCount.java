import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.StringDocValuesReaderState;
import org.apache.lucene.facet.StringValueFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.VectorUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class StringValuesFacetCount {
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
//        Sort indexSort = new Sort(new SortedNumericSortField("number", SortField.Type.LONG, true, SortedNumericSelector.Type.MAX));
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;

        int count = 0 ;
        while (count++ < 10){
            doc = new Document();
            if(count <= 3){
                doc.add(new StringField("content", "a", Field.Store.YES));
                indexWriter.addDocument(doc);
            }else {
                doc.add(new SortedDocValuesField("field", new BytesRef(String.valueOf(count))));

                indexWriter.addDocument(doc);
            }
            if(count % 3 == 0){
            }
        }
//        indexWriter.deleteDocuments(new Term("content", "a"));
        indexWriter.flush();

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        StringDocValuesReaderState state =
                new StringDocValuesReaderState(searcher.getIndexReader(), "field");
        StringValueFacetCounts facets = new StringValueFacetCounts(state);


    }

    private float[] randomVector(int dim, Random random) {
        float[] v = new float[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = random.nextFloat();
        }
        VectorUtil.l2normalize(v);
        return v;
    }
    public static void main(String[] args) throws Exception{
        StringValuesFacetCount test = new StringValuesFacetCount();
        test.doSearch();
    }
}
