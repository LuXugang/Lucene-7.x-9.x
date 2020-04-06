package io.lucene.DcoValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/3/31 4:19 下午
 */
public class PointValuesOneDimensionTest {
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

        Random random = new Random();
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new IntPoint("content", -3));
        doc.add(new IntPoint("content", -5));
        doc.add(new IntPoint("title", 2));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new IntPoint("content", 55));
        doc.add(new IntPoint("title", 3));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new IntPoint("content", 65));
        doc.add(new IntPoint("content", 57));
        indexWriter.addDocument(doc);

        // 1
        doc = new Document();
        doc.add(new IntPoint("content", 10));
        indexWriter.addDocument(doc);

        int count = 0 ;
        while (count++ < 2048){
            doc = new Document();
            int a = random.nextInt(100);
            a = a == 0 ? a + 1 : a;
            doc.add(new IntPoint("content", a));
            doc.add(new IntPoint("title", 10));
            indexWriter.addDocument(doc);


        }

        indexWriter.commit();
    }

    public static void main(String[] args) throws Exception{
        PointValuesOneDimensionTest test = new PointValuesOneDimensionTest();
        test.doSearch();
//    byte[] packed = new byte[100];
//    IntPoint.encodeDimension(256, packed, 0);
//    System.out.println(packed);
    }
}
