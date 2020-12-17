package io.index;

import io.util.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/12/16 14:52
 */
public class PointValuesTest2 {
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
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        Document doc;
        int commitCount = 0;
        while (commitCount++ < 10000){
            // 文档0
            doc = new Document();
            doc.add(new IntPoint("book", 2, 2));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new IntPoint("book", 0, 0));
            doc.add(new IntPoint("book", 1, 1));
            indexWriter.addDocument(doc);
            int count = 0 ;
            int a,c;
            while (count++ < 4096){
                doc = new Document();
                a = random.nextInt(100);
                a = a == 0 ? a + 2 : a;
                c = random.nextInt(80);
                c = c == 0 ? c + 2 : c;
                doc.add(new IntPoint("book", a, c));
                indexWriter.addDocument(doc);
            }
            indexWriter.commit();
        }

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        int [] lowValue = {-1, -1};
        int [] upValue = {60, 60};
        Query query = IntPoint.newRangeQuery("book", lowValue, upValue);
        // 返回Top5的结果
        int resultTopN = 5;

        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN).scoreDocs;

        System.out.println("Total Result Number: "+scoreDocs.length+"");
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            // 输出满足查询条件的 文档号
            System.out.println("result"+i+": 文档"+scoreDoc.doc+"");
        }

    }

    public static void main(String[] args) throws Exception{
        PointValuesTest2 test = new PointValuesTest2();
        test.doSearch();
    }
}
