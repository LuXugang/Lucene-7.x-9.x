package io.search;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
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
 * @date 2020/8/12 4:07 下午
 */
public class IndexOptionTest {

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
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStored(true);
        Document doc ;
        // 文档0
        doc = new Document();
        doc.add(new Field("content", "a", fieldType));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        doc.add(new Field("author", "bcd", fieldType));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        doc.add(new Field("attachment", "ga", fieldType));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new Field("content", "gc", fieldType));
        indexWriter.addDocument(doc);
        // 文档4
        doc = new Document();
        doc.add(new Field("content", "gch",fieldType));
        indexWriter.addDocument(doc);
        // 文档5
        doc = new Document();
        doc.add(new Field("content", "gchb", fieldType));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = new TermRangeQuery("content", new BytesRef("bc"), new BytesRef("gch"), true, true);
        ScoreDoc[] scoreDocs = searcher.search(query, 1000).scoreDocs;
    }

    public static void main(String[] args) throws Exception{
        IndexOptionTest test = new IndexOptionTest();
        test.doSearch();
    }
}
