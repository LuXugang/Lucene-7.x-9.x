package io.lucene.DcoValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/7/10 4:34 下午
 */
public class SortedDocValuesTest4Test {
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

    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        int count = 0;
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new SortedDocValuesField("level", new BytesRef("a")));
        doc.add(new TextField("abc", "document1", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new SortedDocValuesField("level", new BytesRef("d")));
        doc.add(new TextField("abc", "document1", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new SortedDocValuesField("level", new BytesRef("d")));
        doc.add(new TextField("abc", "document2", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new SortedDocValuesField("level", new BytesRef("b")));
        doc.add(new TextField("abc", "document3", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档4
        doc = new Document();
        doc.add(new SortedDocValuesField("level", new BytesRef("c")));
        doc.add(new TextField("abc", "document4", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档5
        doc = new Document();
        doc.add(new TextField("abc", "document5", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = SortedDocValuesField.newSlowRangeQuery("level", new BytesRef("b"), new BytesRef("c"), true, true);
        ScoreDoc[] scoreDocs = searcher.search(query, 10).scoreDocs;
        assert scoreDocs.length == 2;
        assert reader.document(3).get("abc").equals("document3");
        assert scoreDocs[0].doc == 3;
        assert reader.document(scoreDocs[0].doc).get("abc").equals("document3");
        assert scoreDocs[1].doc == 4;
        assert reader.document(scoreDocs[1].doc).get("abc").equals("document4");
    }

    public static void main(String[] args) throws Exception{
        SortedDocValuesTest4Test test4Test = new SortedDocValuesTest4Test();
        test4Test.doIndexAndSearch();
    }
}
