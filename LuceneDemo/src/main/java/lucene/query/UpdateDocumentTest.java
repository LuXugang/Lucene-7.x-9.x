package lucene.query;

import io.FileOperation;
import lucene.index.IndexFileWithLessFieldValues;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/4/26 1:01 下午
 */
public class UpdateDocumentTest {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private IndexWriter indexWriter;

    public void doIndex() throws Exception {
        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        int count = 0;
        while (count++ < 1) {
            // 文档0
            Document doc = new Document();
            doc.add(new Field("author", "aab b aab aabbcc ", type));
            doc.add(new Field("content", "a b", type));
            doc.add(new IntPoint("intPoint", 3, 4, 6));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new TextField("author", "a", Field.Store.YES));
            doc.add(new TextField("content", "a b c h", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", -1));
            doc.add(new IntPoint("intPoint", 3, 5, 6));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
            doc.add(new TextField("content", "a c b e", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", 4));
            indexWriter.addDocument(doc);
            // 文档3
            doc = new Document();
            doc.add(new TextField("author", "aabb ", Field.Store.YES));
            doc.add(new TextField("content", "b c e", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", 1));
            indexWriter.addDocument(doc);
            // 文档4
            doc = new Document();
            doc.add(new TextField("author", "aab", Field.Store.YES));
            doc.add(new TextField("content", "a c e f g d", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档5
            doc = new Document();
            doc.add(new TextField("title", "aab", Field.Store.YES));
            // 更新一个索引中不存在的域名
            indexWriter.updateDocument(new Term("123", "luxugang"), doc);

        }
        indexWriter.commit();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher indexSearcher = new IndexSearcher(reader);
        builder.add(new TermQuery(new Term("title", "aab")), BooleanClause.Occur.SHOULD);

        ScoreDoc doc[]  =  indexSearcher.search(builder.build() , 10).scoreDocs;

        for (ScoreDoc scoreDoc : doc) {
            System.out.printf("docId: "+scoreDoc.doc+"");
        }

    }

    public static void main(String[] args) throws Exception{
        UpdateDocumentTest test = new UpdateDocumentTest();
        test.doIndex();
    }

}
