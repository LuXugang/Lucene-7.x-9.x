package lucene.join;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-01-23 10:43
 */
public class JoinTest {
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

    private void doSearch() throws Exception {
        final String idField = "id";
        final String toField = "productId";
        IndexWriter w = new IndexWriter(directory, conf);

        // 0
        Document doc = new Document();
        doc.add(new TextField("description", "random text", Field.Store.NO));
        doc.add(new TextField("name", "name1", Field.Store.NO));
        doc.add(new TextField(idField, "1", Field.Store.NO));
        doc.add(new SortedDocValuesField(idField, new BytesRef("1")));
        w.addDocument(doc);

        // 1
        doc = new Document();
        doc.add(new TextField("price", "10.0", Field.Store.NO));
        doc.add(new TextField(idField, "2", Field.Store.NO));
        doc.add(new SortedDocValuesField(idField, new BytesRef("2")));
        doc.add(new TextField(toField, "1", Field.Store.NO));
        doc.add(new SortedDocValuesField(toField, new BytesRef("1")));
        w.addDocument(doc);

        // 2
        doc = new Document();
        doc.add(new TextField("price", "20.0", Field.Store.NO));
        doc.add(new TextField(idField, "3", Field.Store.NO));
        doc.add(new SortedDocValuesField(idField, new BytesRef("3")));
        doc.add(new TextField(toField, "1", Field.Store.NO));
        doc.add(new SortedDocValuesField(toField, new BytesRef("1")));
        w.addDocument(doc);

        // 3
        doc = new Document();
        doc.add(new TextField("description", "more random text", Field.Store.NO));
        doc.add(new TextField("name", "name2", Field.Store.NO));
        doc.add(new TextField(idField, "4", Field.Store.NO));
        doc.add(new SortedDocValuesField(idField, new BytesRef("4")));
        w.addDocument(doc);

        // 4
        doc = new Document();
        doc.add(new TextField("price", "10.0", Field.Store.NO));
        doc.add(new TextField(idField, "5", Field.Store.NO));
        doc.add(new SortedDocValuesField(idField, new BytesRef("5")));
        doc.add(new TextField(toField, "4", Field.Store.NO));
        doc.add(new SortedDocValuesField(toField, new BytesRef("4")));
        w.addDocument(doc);

        // 5
        doc = new Document();
        doc.add(new TextField("price", "20.0", Field.Store.NO));
        doc.add(new TextField(idField, "6", Field.Store.NO));
        doc.add(new SortedDocValuesField(idField, new BytesRef("6")));
        doc.add(new TextField(toField, "4", Field.Store.NO));
        doc.add(new SortedDocValuesField(toField, new BytesRef("4")));
        w.addDocument(doc);
        w.commit();

        IndexReader reader = DirectoryReader.open(w);
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        Query joinQuery =
                JoinUtil.createJoinQuery(idField, false, "productId", new TermQuery(new Term("name", "name2")), indexSearcher, ScoreMode.None);
        TopDocs result = indexSearcher.search(joinQuery, 10);
//        assertEquals(2, result.totalHits);
//        assertEquals(4, result.scoreDocs[0].doc);
//        assertEquals(5, result.scoreDocs[1].doc);

        joinQuery = JoinUtil.createJoinQuery(idField, false, toField, new TermQuery(new Term("name", "name1")), indexSearcher, ScoreMode.None);
        result = indexSearcher.search(joinQuery, 10);
//        assertEquals(2, result.totalHits);
//        assertEquals(1, result.scoreDocs[0].doc);
//        assertEquals(2, result.scoreDocs[1].doc);

        joinQuery = JoinUtil.createJoinQuery(toField, false, idField, new TermQuery(new Term("id", "5")), indexSearcher, ScoreMode.None);
        result = indexSearcher.search(joinQuery, 10);
//        assertEquals(1, result.totalHits);
//        assertEquals(3, result.scoreDocs[0].doc);

        System.out.println("Hello World");

    }

    public static void main(String[] args) throws Exception{
        JoinTest test = new JoinTest();
        test.doSearch();
    }


}
