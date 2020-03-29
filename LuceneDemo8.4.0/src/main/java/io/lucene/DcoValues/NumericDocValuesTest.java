package io.lucene.DcoValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/3/19 1:13 下午
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

    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        int count = 0;
        while (count++ < 1) {
            // 文档0
            Document doc = new Document();
            doc.add(new NumericDocValuesField("age", 88L));
            doc.add(new NumericDocValuesField("level", 10L));
            indexWriter.addDocument(doc);

            // 文档1
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 92));
            indexWriter.addDocument(doc);

            // 文档2
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 4L));
            doc.add(new TextField("abc", "document2", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 文档3
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 24L));
            doc.add(new TextField("abc", "document3", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 文档4
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 20L));
            indexWriter.addDocument(doc);

            // 文档5
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 32L));
            indexWriter.addDocument(doc);

            // 文档6
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 42L));
            indexWriter.addDocument(doc);

            // 文档7
            doc = new Document();
            doc.add(new StringField("abcd", "good", Field.Store.YES));
            indexWriter.addDocument(doc);

            indexWriter.commit();
        }
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        Sort sort = new Sort(new SortField("age", SortField.Type.INT));
        TopDocs docs = searcher.search(new MatchAllDocsQuery(), 1000 , sort);

        for (ScoreDoc scoreDoc: docs.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }

    }

    public static void main(String[] args) throws Exception{
        NumericDocValuesTest test = new NumericDocValuesTest();
        test.doIndexAndSearch();
    }
}
