package io.index;

import io.util.FileOperation;
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
 * @date 2021/5/26 10:11 上午
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
    IndexWriter indexWriter;

    public void doIndexAndSearch() throws Exception {
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setMaxCommitMergeWaitMillis(0);
        conf.setMergeScheduler(new SerialMergeScheduler());
//        conf.setMergePolicy(new LogDocMergePolicy());
        String sortedField = "sortByNumber";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Document doc ;
        int count = 0;
        while (count++ < 5){
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "abc", type));
            doc.add(new Field("content", "cd", type));
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
            doc.add(new NumericDocValuesField(sortedField, 3L));
            doc.add(new StoredField("author", 3));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
            doc.add(new NumericDocValuesField(sortedField, 1L));
            indexWriter.addDocument(doc);
            if(count == 5)
                indexWriter.deleteDocuments(new Term("content", "abc"));
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", type));
            doc.add(new NumericDocValuesField(sortedField, 2L));
            indexWriter.addDocument(doc);
            indexWriter.flush();
        }
//        indexWriter.forceMerge(1);
    }

    public static void main(String[] args) throws Exception{
        ForceMergeTest test = new ForceMergeTest();
        test.doIndexAndSearch();
    }
}
