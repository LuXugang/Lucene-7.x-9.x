import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class TestSortedNumericDocValuesSetQuery {

    public void doSearch() throws Exception {
        Directory directory;
        FileOperation.deleteFile("./data");
        directory = new MMapDirectory(Paths.get("./data"));
        IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(new WhitespaceAnalyzer()));
        // 文档0
        Document document = new Document();
        document.add(new SortedNumericDocValuesField("name", 1));
        document.add(new SortedNumericDocValuesField("name", 2));
        document.add(new SortedNumericDocValuesField("name", 5));
        indexWriter.addDocument(document);
        // 文档1
        document = new Document();
        document.add(new SortedNumericDocValuesField("name", 1));
        indexWriter.addDocument(document);
        // 文档2
        document = new Document();
        document.add(new SortedNumericDocValuesField("name", 2));
        document.add(new SortedNumericDocValuesField("name", 3));
        indexWriter.addDocument(document);
        indexWriter.commit();

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        long[] values = new long[]{2, 3};
        TopDocs topDocs = searcher.search(NumericDocValuesField.newSlowSetQuery("name", values), 100);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            System.out.println("doc id: " + scoreDoc.doc);
        }
        indexWriter.close();
        reader.close();
        directory.close();
        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TestSortedNumericDocValuesSetQuery test = new TestSortedNumericDocValuesSetQuery();
        test.doSearch();
    }
}
