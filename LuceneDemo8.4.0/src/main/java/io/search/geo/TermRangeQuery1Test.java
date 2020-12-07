package io.search.geo;

import io.FileOperation;
import io.search.TermRangeQueryTest;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/8/21 1:53 下午
 */
public class TermRangeQuery1Test {

    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Analyzer analyzer = new WhitespaceAnalyzer();
    private final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        Document doc ;
        // 文档0
        doc = new Document();
        doc.add(new TextField("content", "a", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new TextField("content", "bcd", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new TextField("content", "ga", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new TextField("content", "gc", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档4
        doc = new Document();
        doc.add(new TextField("content", "gch", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档5
        doc = new Document();
        doc.add(new TextField("content", "gchb", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = new TermRangeQuery("content", new BytesRef("bc"), new BytesRef("gch"), true, true);
        ScoreDoc[] scoreDocs = searcher.search(query, 1000).scoreDocs;
    }

    public static void main(String[] args) throws Exception{
        TermRangeQuery1Test test = new TermRangeQuery1Test();
        test.doSearch();
    }

}
