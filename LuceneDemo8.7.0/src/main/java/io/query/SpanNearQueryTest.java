package io.query;

import io.index.StoredFieldTest;
import io.util.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Lu Xugang
 * @date 2020/11/6 1:46 PM
 */
public class SpanNearQueryTest {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    IndexWriter indexWriter;

    public void doIndexAndSearch() throws Exception {
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        Document doc ;
        int count = 0;
        while (count++ < 1){
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "you are good boy", type));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new Field("content", "you good boy", type));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "you are really good boy", type));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new Field("content", "boy good you", type));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        SpanQuery q1  = new SpanTermQuery(new Term("content", "you"));
        SpanQuery q2  = new SpanTermQuery(new Term("content", "boy"));
        Query q = new SpanNearQuery(new SpanQuery[]{q1, q2}, 1, false);
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        ScoreDoc[] scoreDocs = indexSearcher.search(q, 100).scoreDocs;
        System.out.printf("abc");
    }

    public static void main(String[] args) throws Exception{
        SpanNearQueryTest test = new SpanNearQueryTest();
        test.doIndexAndSearch();
    }
}
