package io.DocValues;

import io.util.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/11/19 下午1:57
 */
public class BinaryDocValuesTest {
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
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new BinaryDocValuesField("level", new BytesRef("a df")));
        doc.add(new TextField("abc", "document0", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new BinaryDocValuesField("level", new BytesRef("dc")));
        doc.add(new TextField("abc", "document1", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new TextField("abc", "document2", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new BinaryDocValuesField("level", new BytesRef("da")));
        doc.add(new TextField("abc", "document3", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Sort sortByLevel = new Sort(new SortField("level", SortField.Type.STRING_VAL, false));
        ScoreDoc[] scoreDoc= searcher.search(new MatchAllDocsQuery(), 4, sortByLevel).scoreDocs;
        assert scoreDoc.length == 4;
        assert scoreDoc[0].doc == 2;
        assert reader.document(scoreDoc[0].doc).get("abc").equals("document2");
        assert scoreDoc[1].doc == 0;
        assert reader.document(scoreDoc[1].doc).get("abc").equals("document0");
        assert scoreDoc[2].doc == 3;
        assert reader.document(scoreDoc[2].doc).get("abc").equals("document3");
        assert scoreDoc[3].doc == 1;
        assert reader.document(scoreDoc[3].doc).get("abc").equals("document1");
    }

    public static void main(String[] args) throws Exception{
        BinaryDocValuesTest test = new BinaryDocValuesTest();
        test.doIndexAndSearch();
    }
}
