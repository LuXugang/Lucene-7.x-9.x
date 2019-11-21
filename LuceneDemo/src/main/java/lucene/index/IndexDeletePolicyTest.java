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
 * @date 2019-02-21 09:58
 */
public class IndexDeletePolicyTest {
    private Directory directory ;
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;
    private PersistentSnapshotDeletionPolicy persistentSnapshotDeletionPolicy;
    private SnapshotDeletionPolicy snapshotDeletionPolicy;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = FSDirectory.open(Paths.get("./data"));
//            conf.setUseCompoundFile(true);
            persistentSnapshotDeletionPolicy = new PersistentSnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy(), directory);
            snapshotDeletionPolicy = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
            conf.setIndexDeletionPolicy(persistentSnapshotDeletionPolicy);
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

        IndexCommit indexCommit = null;
        int count = 0;
        Document doc;
        while (count++ < 3) {
            // 文档0
            doc = new Document();
            doc.add(new Field("author", "Lucy", type));
            doc.add(new Field("title", "notCare", type));
            doc.add(new IntPoint("pointValue", 3, 4, 5));
            doc.add(new NumericDocValuesField("docValuesField", 8));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new Field("author", "Lily", type));
            doc.add(new StringField("title", "Care", Field.Store.YES));
            doc.add(new NumericDocValuesField("docValuesField", 3));
            indexWriter.addDocument(doc);

            indexWriter.commit();
            if(count == 2){
                indexCommit = persistentSnapshotDeletionPolicy.snapshot();
                System.out.println("abc");
            }
        }


        doc = new Document();
        doc.add(new Field("author", "Lucy", type));
        doc.add(new Field("title", "notCare", type));
        doc.add(new IntPoint("pointValue", 3, 4, 5));
        doc.add(new NumericDocValuesField("docValuesField", 8));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        indexWriter.close();

        IndexWriterConfig newConf = new IndexWriterConfig(analyzer);
        newConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        newConf.setIndexCommit(indexCommit);
        IndexWriter newIndexWriter = new IndexWriter(directory, newConf);
        System.out.println("abc");


    }
    public static void main(String[] args) throws Exception{
        IndexDeletePolicyTest test = new IndexDeletePolicyTest();
        test.doIndex();
    }
}
