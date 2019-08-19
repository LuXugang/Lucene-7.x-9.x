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

import java.io.IOException;
import java.nio.file.Paths;

public class PhraseQueryTest {
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
        doc.add(new TextField("content", "a quick black fox", Field.Store.YES));
        doc.add(new TextField("author", "author1", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new TextField("content", "a really quick red fox", Field.Store.YES));
        doc.add(new TextField("author", "author2", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new TextField("content", "quick fox", Field.Store.YES));
        doc.add(new TextField("author", "author3", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new TextField("content", "fox quick", Field.Store.YES));
        doc.add(new TextField("author", "author3", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        // 索引阶段结束

        //查询阶段开始
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.add(new Term("content", "quick"), 1);
        builder.add(new Term("content", "fox"), 3);
        builder.setSlop(4);
        Query query = builder.build();
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
        PhraseQueryTest phraseQueryTest = new PhraseQueryTest();
        phraseQueryTest.doDemo();
    }
}
