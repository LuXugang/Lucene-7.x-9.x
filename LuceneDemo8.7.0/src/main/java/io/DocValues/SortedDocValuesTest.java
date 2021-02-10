package io.DocValues;

import io.util.FileOperation;
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
 * @date 2021/2/9 10:16 上午
 */
public class SortedDocValuesTest {
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
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Document doc;
        int commitCount = 0;
        while (commitCount++ < 100000){
            // 文档0
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("a")));
//            doc.add(new BinaryDocValuesField("level1", new BytesRef("df")));
//            doc.add(new SortedDocValuesField("alevl", new BytesRef("adf")));
            doc.add(new TextField("abc", "document0", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("d")));
//            doc.add(new BinaryDocValuesField("level1", new BytesRef("da")));
//            doc.add(new SortedDocValuesField("alevl", new BytesRef("df")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new TextField("abc", "document2", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档3
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("c")));
//            doc.add(new BinaryDocValuesField("level1", new BytesRef("da")));
//            doc.add(new TextField("abc", "document3", Field.Store.YES));
            indexWriter.addDocument(doc);
            indexWriter.commit();
        }

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Sort sortByLevel = new Sort(new SortField("level", SortField.Type.STRING_VAL, false));
        ScoreDoc[] scoreDoc= searcher.search(new MatchAllDocsQuery(), 2, sortByLevel).scoreDocs;
    }

    public static void main(String[] args) throws Exception{
        SortedDocValuesTest test = new SortedDocValuesTest();
        test.doIndexAndSearch();
    }
}
