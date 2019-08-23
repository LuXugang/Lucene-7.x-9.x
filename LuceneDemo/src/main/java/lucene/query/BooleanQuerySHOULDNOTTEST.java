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

/**
 * @author Lu Xugang
 * @date 2019/8/22 4:08 下午
 */
public class BooleanQuerySHOULDNOTTEST {
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

    // 空格分词器
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);

    private void doDemo() throws Exception {
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        Document doc ;
        // 文档0
        doc = new Document();
        doc.add(new TextField("content", "h a h", Field.Store.YES));
        doc.add(new TextField("author", "author1", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new TextField("content", "f f", Field.Store.YES));
        doc.add(new TextField("author", "author2", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new TextField("content", "h a c", Field.Store.YES));
        doc.add(new TextField("author", "author3", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new TextField("content", "a h h e", Field.Store.YES));
        doc.add(new TextField("author", "author4", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档4
        doc = new Document();
        doc.add(new TextField("content", "a f h", Field.Store.YES));
        doc.add(new TextField("author", "author5", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档5
        doc = new Document();
        doc.add(new TextField("content", "h", Field.Store.YES));
        doc.add(new TextField("author", "author6", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档6
        doc = new Document();
        doc.add(new TextField("content", "c a", Field.Store.YES));
        doc.add(new TextField("author", "author7", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档7
        doc = new Document();
        doc.add(new TextField("content", "f f f", Field.Store.YES));
        doc.add(new TextField("author", "author8", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档8
        doc = new Document();
        doc.add(new TextField("content", "e a d f h a a ", Field.Store.YES));
        doc.add(new TextField("author", "author9", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档9
        doc = new Document();
        doc.add(new TextField("content", "a c a b c", Field.Store.YES));
        doc.add(new TextField("author", "author10", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        // 索引阶段结束

        // 查询阶段
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        // 子查询1
        builder.add(new TermQuery(new Term("content", "h")), BooleanClause.Occur.SHOULD);// 包含"h"的文档共6篇：0、2、3、4、5、8
        // 子查询2
        builder.add(new TermQuery(new Term("content", "f")), BooleanClause.Occur.SHOULD);// 包含"f"的文档共4篇：1、4、7、8
        // 子查询3
        builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);// 包含"a"的文档共7篇：0、2、3、4、6、8、9
        // 子查询4
        builder.add(new TermQuery(new Term("content", "e")), BooleanClause.Occur.MUST_NOT);// 包含"e"的文档共2篇：3、8
        builder.setMinimumNumberShouldMatch(2);
        Query query = builder.build();

        // 返回Top5的结果
        int resultTopN = 10;
//
        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN).scoreDocs;

        System.out.println("Total Result Number: "+scoreDocs.length+"");
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
//            // 输出满足查询条件的 文档号
            System.out.println("result"+i+": 文档"+scoreDoc.doc+", "+scoreDoc.score+"");
        }
    }

    public static void main(String[] args) throws Exception{
        BooleanQuerySHOULDNOTTEST booleanQuerySHOULDNOTTEST = new BooleanQuerySHOULDNOTTEST();
        booleanQuerySHOULDNOTTEST.doDemo();
    }
}
