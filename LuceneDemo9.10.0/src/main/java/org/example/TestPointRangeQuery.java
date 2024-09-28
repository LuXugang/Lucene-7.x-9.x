package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestPointRangeQuery {
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

    public void doIndex() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        Document doc;
        // 0
        doc = new Document();
        doc.add(new IntPoint("content", 3));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new IntPoint("content", 10));
        indexWriter.addDocument(doc);


        doc = new Document();
//    doc.add(new IntPoint("title", 10, 55));
        indexWriter.addDocument(doc);

        int count = 0 ;
        while (count++ < 2048){
            doc = new Document();
            int a = random.nextInt(100);
            a = a == 0 ? a + 1 : a;
            int b = random.nextInt(100);
            b = b == 0 ? b + 1 : b;
            doc.add(new IntPoint("content", a));
            indexWriter.addDocument(doc);
        }

        indexWriter.commit();
        DirectoryReader r = DirectoryReader.open(indexWriter);
        IndexSearcher s = new IndexSearcher(r);

        int [] lowValue = {3};
        int [] upValue = {25};
//
//    int [] lowValue = {-1, -1};
//    int [] upValue = {100, 100};

        int num;
        TopDocs topDocs = s.search(IntPoint.newRangeQuery("content", lowValue, upValue), 100);
        num = s.count(IntPoint.newRangeQuery("content", lowValue, upValue));
        System.out.println("result number : "+ num +"");


        // Per-top-reader state:k
    }



    public static void main(String[] args) throws Exception{
        TestPointRangeQuery query = new TestPointRangeQuery();
        query.doIndex();
    }
}
