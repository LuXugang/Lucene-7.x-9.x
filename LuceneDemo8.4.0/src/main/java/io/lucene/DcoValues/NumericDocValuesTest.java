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
            doc.add(new NumericDocValuesField("age", 88));
            doc.add(new NumericDocValuesField("level", 10));
            doc.add(new TextField("abc", "document0", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 92));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new NumericDocValuesField("age", 4));
            doc.add(new NumericDocValuesField("level", 3));
            doc.add(new TextField("abc", "document2", Field.Store.YES));
            indexWriter.addDocument(doc);
            indexWriter.commit();
        }
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        Sort sortByAge = new Sort(new SortField("age", SortField.Type.INT, true));
        Sort sortByLevel = new Sort(new SortField("level", SortField.Type.INT, true));
        TopDocs docs1 = searcher.search(new MatchAllDocsQuery(), 1000 , sortByAge);
        TopDocs docs2 = searcher.search(new MatchAllDocsQuery(), 1000 , sortByLevel);

        System.out.println("sort by age");
        for (ScoreDoc scoreDoc: docs1.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }

        System.out.println("sort by level");
        for (ScoreDoc scoreDoc: docs2.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }

    }

    public static void main(String[] args) throws Exception{
        NumericDocValuesTest test = new NumericDocValuesTest();
        test.doIndexAndSearch();
    }
}
