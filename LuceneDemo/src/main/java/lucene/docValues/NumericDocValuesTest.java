package lucene.docValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-02 22:39
 */
public class NumericDocValuesTest {
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

        String groupField1 = "age";
        String groupField2 = "b";
        // 0
        int count = 0;
        while (count++ < 80000) {
            Document doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 88L));
            doc.add(new StringField("abcd", "good", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 1
            doc = new Document();
            doc.add(new StringField("abcd", "good", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 2
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 4L));
            doc.add(new TextField("abc", "value", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 3
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 24L));
            doc.add(new TextField("abc", "value", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 4
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 20L));
            indexWriter.addDocument(doc);

            // 5
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 32L));
            indexWriter.addDocument(doc);

            // 6
            doc = new Document();
            doc.add(new NumericDocValuesField(groupField1, 42L));
            doc.add(new StringField("abcd", "good", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 7
            doc = new Document();
            doc.add(new StringField("abcd", "good", Field.Store.YES));
            indexWriter.addDocument(doc);


//
        }

        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        Sort sortByLevel = new Sort(new SortField(groupField1, SortField.Type.INT, true));
        TopDocs docs2 = searcher.search(new MatchAllDocsQuery(), 1000 , sortByLevel);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("abcd", "good")), BooleanClause.Occur.MUST);

        for (ScoreDoc scoreDoc: docs2.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }
    }

    public static void main(String[] args) throws Exception{
        NumericDocValuesTest test = new NumericDocValuesTest();
        test.doSearch();
    }
}
