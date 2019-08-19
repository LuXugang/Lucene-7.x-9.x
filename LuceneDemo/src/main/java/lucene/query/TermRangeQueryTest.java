package lucene.query;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

public class TermRangeQueryTest {
    private Directory directory;

    {
        try {
            // 每次运行demo先清空索引目录中的索引文件
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void doDemo() throws Exception {

        // 查询阶段开始
        // 空格分词器
        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, conf);

        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new TextField("content", "a", Field.Store.YES));
        doc.add(new TextField("name", "Cris", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new TextField("content", "bcd", Field.Store.YES));
        doc.add(new TextField("name", "Andy", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new TextField("content", "ga", Field.Store.YES));
        doc.add(new TextField("name", "Jack", Field.Store.YES));
        indexWriter.addDocument(doc);

        // 文档4
        doc = new Document();
        doc.add(new TextField("content", "gc", Field.Store.YES));
        doc.add(new TextField("name", "Pony", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档5
        doc = new Document();
        doc.add(new TextField("content", "gch", Field.Store.YES));
        doc.add(new TextField("name", "Jolin", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档6
        doc = new Document();
        doc.add(new TextField("content", "gchb", Field.Store.YES));
        doc.add(new TextField("name", "Jay", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        // 索引阶段结束
        // 查询阶段开始
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new TermRangeQuery("content", new BytesRef("bc"), new BytesRef("gc"), true, true);


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
        TermRangeQueryTest termRangeQueryTest = new TermRangeQueryTest();
        termRangeQueryTest.doDemo();
    }
}
