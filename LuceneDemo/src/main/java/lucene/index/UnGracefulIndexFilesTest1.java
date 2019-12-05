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
 * @date 2019/12/5 11:13 上午
 */
public class UnGracefulIndexFilesTest1 {
    private Directory directory ;
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig oldConf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;


    {
        try {
            FileOperation.deleteFile("./data");
            directory = FSDirectory.open(Paths.get("./data"));
            oldConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            oldConf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
            oldConf.setMergePolicy(NoMergePolicy.INSTANCE);
            oldConf.setCommitOnClose(false);
            indexWriter = new IndexWriter(directory, oldConf);
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
        doc = new Document();
        doc.add(new StringField("author", "Lily", Field.Store.YES));
        doc.add(new NumericDocValuesField("age", -2));
        indexWriter.addDocument(doc);
        // 生成segments_1文件
        indexWriter.commit();

        doc = new Document();
        doc.add(new StringField("author", "Lily", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.prepareCommit();
        System.out.println("hah");
    }
    public static void main(String[] args) throws Exception {
        UnGracefulIndexFilesTest1 unGracefulIndexFilesTest1 = new UnGracefulIndexFilesTest1();
        unGracefulIndexFilesTest1.doIndex();
    }
}
