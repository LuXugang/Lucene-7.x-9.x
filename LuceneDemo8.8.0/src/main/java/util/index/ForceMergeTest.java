package util.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2021/5/25 7:56 下午
 */
public class ForceMergeTest {
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

    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        String sortedField = "sortByNumber";
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setTokenized(true);
        fieldType.setStored(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setStoreTermVectors(true);
        Document doc;
        int count = 0;
        while (count++ < 5){
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "abc", fieldType));
            doc.add(new Field("content", "cd", fieldType));
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
//            doc.add(new NumericDocValuesField(sortedField, 3L));
            doc.add(new StoredField("author", 3));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
            doc.add(new NumericDocValuesField(sortedField, 1L));
            indexWriter.addDocument(doc);
//            if(count == 2){
//                doc = new Document();
//                doc.add(new StringField("attachment", "cd", Field.Store.NO));
//                doc.add(new NumericDocValuesField(sortedField, 1L));
//                indexWriter.addDocument(doc);
//            }
            if(count == 2)
                indexWriter.deleteDocuments(new Term("content", "abc"));
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", fieldType));
            doc.add(new NumericDocValuesField(sortedField, 2L));
            indexWriter.addDocument(doc);
            // 文档4
            doc = new Document();
            doc.add(new Field("content", "the name is name", fieldType));
            doc.add(new NumericDocValuesField(sortedField, 2L));
            indexWriter.addDocument(doc);
            indexWriter.commit();
        }
//        IndexReader reader = DirectoryReader.open(indexWriter);
        indexWriter.forceMerge(2);
        indexWriter.commit();
//        IndexSearcher searcher = new IndexSearcher(reader);
//        searcher.search(new MatchAllDocsQuery(), 10);

    }

    public static void main(String[] args) throws Exception{
        ForceMergeTest test = new ForceMergeTest();
        test.doIndexAndSearch();
    }
}
