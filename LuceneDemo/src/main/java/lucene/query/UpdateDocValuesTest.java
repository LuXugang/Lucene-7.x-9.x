package lucene.query;

import io.FileOperation;
import lucene.index.IndexFileWithManyFieldValues;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019/9/2 7:32 下午
 */
public class UpdateDocValuesTest {
    private Directory directory;
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = FSDirectory.open(Paths.get("./data"));
            conf.setUseCompoundFile(false);
//            SortField sortField = new SortedNumericSortField("docValuesField", SortField.Type.INT);
//            Sort sort = new Sort(sortField);
//            conf.setIndexSort(sort);
            indexWriter = new IndexWriter(directory, conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void doIndex() throws Exception {

        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        Document doc;

        // 文档0
        doc = new Document();
        doc.add(new Field("author", "Lily", type));
        doc.add(new Field("title", "notCare", type));
        doc.add(new IntPoint("pointValue", 3, 4, 5));
        doc.add(new Field("sex", "man", type));
        doc.add(new NumericDocValuesField("docValuesField", 8));
        indexWriter.addDocument(doc);

        // 文档1
        doc = new Document();
        doc.add(new Field("author", "Lily", type));
        doc.add(new StringField("title", "maybe", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesField", 3));
        indexWriter.addDocument(doc);

        // 文档2
        doc = new Document();
        doc.add(new Field("author", "papa", type));
        doc.add(new StringField("title", "maybe", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesField", 10));
        indexWriter.addDocument(doc);

        // 文档3
        doc = new Document();
        doc.add(new Field("author", "Jay", type));
        doc.add(new StringField("title", "sure", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesField", 9));
        indexWriter.softUpdateDocument(new Term("author", "Lily"), doc, new NumericDocValuesField("docValuesField", 1));
//       indexWriter.updateNumericDocValue(new Term("author", "Lily"), "docValuesField", 11) ;


        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);

        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = new MatchAllDocsQuery();

        SortField sortField  = new SortedNumericSortField("docValuesField", SortField.Type.INT);

        Sort sort = new Sort(sortField);

        ScoreDoc[] scoreDocs = searcher.search(query, 10, sort).scoreDocs;
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            // 输出满足查询条件的 文档号
            System.out.println("result"+i+": 文档"+scoreDoc.doc+"");
        }
        System.out.println("hah");
    }

    public static void main(String[] args) throws Exception{
        UpdateDocValuesTest test = new UpdateDocValuesTest();
        test.doIndex();
    }
}
