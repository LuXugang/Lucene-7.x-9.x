package io.search;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/7/10 3:15 下午
 */
public class TermRangeQueryTest {

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
//
        Document doc ;

        int count = 0;
        // 0
        doc = new Document();
        doc.add(new TextField("content", "a", Field.Store.YES));
        doc.add(new TextField("name", "Cris", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new TextField("content", "bcd", Field.Store.YES));
        doc.add(new TextField("name", "Andy", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new TextField("content", "ga", Field.Store.YES));
        doc.add(new TextField("name", "Jack", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 3
        doc = new Document();
        doc.add(new TextField("content", "gb", Field.Store.YES));
        doc.add(new TextField("name", "Jack", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 4
        doc = new Document();
        doc.add(new TextField("content", "gc", Field.Store.YES));
        doc.add(new TextField("name", "Pony", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 5
        doc = new Document();
        doc.add(new TextField("content", "gch", Field.Store.YES));
        doc.add(new TextField("name", "Jolin", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 6
        doc = new Document();
        doc.add(new TextField("content", "gchb", Field.Store.YES));
        doc.add(new TextField("name", "Jay", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new TermRangeQuery("content", new BytesRef("ga"), new BytesRef("gch"), true, true);
        ScoreDoc[] scoreDocs = searcher.search(query, 1000).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println(scoreDoc.doc );
        }
    }

    public static void main(String[] args) throws Exception{
        TermRangeQueryTest test = new TermRangeQueryTest();
        test.doSearch();
    }

}
