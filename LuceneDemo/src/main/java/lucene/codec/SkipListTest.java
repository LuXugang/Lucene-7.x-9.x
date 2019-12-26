package lucene.codec;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/12/16 8:39 下午
 */
public class SkipListTest {

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

        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        Document doc ;
        // 0

        int count = 0;
        while (count++ < 7999) {
            doc = new Document();
            doc.add(new Field("content", "a e c", type));
            doc.add(new Field("author", "e", type));
            indexWriter.addDocument(doc);
            // 1
            doc = new Document();
            doc.add(new Field("content", "e", type));
            doc.add(new Field("buthor", "a d c", type));
            indexWriter.addDocument(doc);
            // 2
            doc = new Document();
            doc.add(new Field("content", "c", type));
            doc.add(new Field("author", "a a d c", type));
            indexWriter.addDocument(doc);
            // 3
            doc = new Document();
            doc.add(new Field("content", "a c e", type));
            indexWriter.addDocument(doc);
            // 4
            doc = new Document();
            doc.add(new Field("content", "h", type));
            indexWriter.addDocument(doc);
            // 5
            doc = new Document();
            doc.add(new Field("content", "b h", type));
            indexWriter.addDocument(doc);
            // 6
            doc = new Document();
            doc.add(new Field("content", "c a", type));
            indexWriter.addDocument(doc);
            // 7
            doc = new Document();
            doc.add(new Field("content", "a e h", type));
            indexWriter.addDocument(doc);
            // 8
            doc = new Document();
            doc.add(new Field("content", "b c d e h e", type));
            indexWriter.addDocument(doc);
            // 9
            doc = new Document();
            doc.add(new Field("content", "a e a b ", type));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "d")), BooleanClause.Occur.MUST);
        builder.setMinimumNumberShouldMatch(2);
        Query query = builder.build();

        int topN = 10;

        ScoreDoc[] scoreDocs = searcher.search(query, topN).scoreDocs;


        System.out.println("Total Result Number: "+scoreDocs.length+"");
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
//            // 输出满足查询条件的 文档号
            System.out.println("result"+i+": 文档"+scoreDoc.doc+", "+scoreDoc.score+"");
        }
    }

    public static void main(String[] args) throws Exception{
        SkipListTest test = new SkipListTest();
        test.doSearch();
    }

}
