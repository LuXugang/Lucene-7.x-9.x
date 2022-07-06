import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class TieBreakDocIdKNNQuery {
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

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
            try (IndexWriter w = new IndexWriter(directory, new IndexWriterConfig())) {
                for (int j = 0; j < 5; j++) {
                    Document doc = new Document();
                    doc.add(
                            new KnnVectorField("field", new float[] {0, 1}, VectorSimilarityFunction.DOT_PRODUCT));
                    w.addDocument(doc);
                }
            }
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                KnnVectorQuery query = new KnnVectorQuery("field", new float[] {2, 3}, 100);
                ScoreDoc[] scoreDocs = searcher.search(query, 30).scoreDocs;
                for (ScoreDoc scoreDoc : scoreDocs) {
                    System.out.println("docId: " + scoreDoc.doc + "");
                }
            }

    }
    public static void main(String[] args) throws Exception{
        TieBreakDocIdKNNQuery test = new TieBreakDocIdKNNQuery();
        test.doSearch();
    }
}
