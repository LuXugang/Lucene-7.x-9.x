package lucene.query;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/10/11 5:32 下午
 */
public class MoreLikeThisTest {

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
        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        IndexWriter indexWriter = new IndexWriter(directory, conf);
        Document doc ;
        // 0
        doc = new Document();
        doc.add(new Field("content", "h", type));
        doc.add(new Field("author", "author1", type));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new Field("content", "b", type));
        doc.add(new Field("author", "author2", type));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new Field("content", "a c b", type));
        doc.add(new Field("author", "author3", type));

        indexWriter.addDocument(doc);
        // 3
        doc = new Document();
        doc.add(new Field("content", "a c e", type));
        doc.add(new Field("author", "author4", type));
        indexWriter.addDocument(doc);
        // 4
        doc = new Document();
        doc.add(new Field("content", "h", type));
        doc.add(new Field("author", "author5", type));
        indexWriter.addDocument(doc);
        // 5
        doc = new Document();
        doc.add(new Field("content", "c e", type));
        doc.add(new Field("author", "author6", type));
        indexWriter.addDocument(doc);
        // 6
        doc = new Document();
        doc.add(new Field("content", "c a e", type));
        doc.add(new Field("author", "author7", type));
        indexWriter.addDocument(doc);
        // 7
        doc = new Document();
        doc.add(new Field("content", "f", type));
        doc.add(new Field("author", "author8", type));
        indexWriter.addDocument(doc);
        // 8
        doc = new Document();
        doc.add(new Field("content", "b c d e c e", type));
        doc.add(new Field("author", "author9", type));
        indexWriter.addDocument(doc);
        // 9
        doc = new Document();
        doc.add(new Field("content", "a c e a b c", type));
        doc.add(new Field("author", "author10", type));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        // 索引阶段结束

        //查询阶段
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        MoreLikeThis moreLikeThis = new MoreLikeThis(reader);
        String[] fieldNames = {"content"};
        moreLikeThis.setFieldNames(fieldNames);
//        moreLikeThis.setFieldNames(null);
        moreLikeThis.setAnalyzer(analyzer);

    Query query = moreLikeThis.like(9);
//        Query query = moreLikeThis.like();
//        Query query = moreLikeThis.like();

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
        MoreLikeThisTest test = new MoreLikeThisTest();
        test.doDemo();
    }
}
