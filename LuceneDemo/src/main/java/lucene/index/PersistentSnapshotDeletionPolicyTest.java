package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/12/5 10:09 下午
 */
public class PersistentSnapshotDeletionPolicyTest {
    private Directory directory ;
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter oldIndexWriter;
    private PersistentSnapshotDeletionPolicy persistentSnapshotDeletionPolicy;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = FSDirectory.open(Paths.get("./data"));
            persistentSnapshotDeletionPolicy = new PersistentSnapshotDeletionPolicy(NoDeletionPolicy.INSTANCE, directory);
            conf.setIndexDeletionPolicy(persistentSnapshotDeletionPolicy);
            oldIndexWriter = new IndexWriter(directory, conf);
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
        doc.add(new Field("author", "Lucy", type));
        doc.add(new NumericDocValuesField("docValuesField", 8));
        oldIndexWriter.addDocument(doc);
        oldIndexWriter.commit();
        // 文档1
        doc = new Document();
        doc.add(new Field("author", "Lucy", type));
        doc.add(new NumericDocValuesField("docValuesField", 3));
        oldIndexWriter.addDocument(doc);
        oldIndexWriter.commit();
        IndexCommit indexCommit = persistentSnapshotDeletionPolicy.snapshot();

        // 文档2
        doc = new Document();
        doc.add(new Field("author", "Jay", type));
        doc.add(new IntPoint("pointValue", 3, 4, 5));
        oldIndexWriter.addDocument(doc);
        oldIndexWriter.deleteDocuments(new Term("author", "Lucy"));
        oldIndexWriter.commit();
        oldIndexWriter.close();

        IndexWriterConfig newConf = new IndexWriterConfig(analyzer);
        newConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        newConf.setIndexCommit(indexCommit);
        IndexWriter newIndexWriter = new IndexWriter(directory, newConf);
    }
    public static void main(String[] args) throws Exception{
        PersistentSnapshotDeletionPolicyTest test = new PersistentSnapshotDeletionPolicyTest();
        test.doIndex();
    }
}
