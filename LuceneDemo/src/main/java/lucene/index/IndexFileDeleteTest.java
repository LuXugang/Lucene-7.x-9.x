package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/11/27 4:26 下午
 */
public class IndexFileDeleteTest {
    private Directory directory;
    private IndexWriter indexWriter;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
            Analyzer analyzer = new WhitespaceAnalyzer();
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            conf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
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


        Document doc = new Document();

        doc.add(new Field("author", "kun", type));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        doc.add(new Field("author", "cai", type));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        doc = new Document();
        doc.add(new Field("author", "Jay", type));
        indexWriter.addDocument(doc);
//        indexWriter.deleteDocuments(new Term("author", "kun"));
        indexWriter.commit();

        System.out.println("abc");

    }

    public static void main(String[] args) throws Throwable{
        IndexFileDeleteTest test = new IndexFileDeleteTest();
        test.doIndex();
    }
}
