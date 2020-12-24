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
import java.util.List;

/**
 * @author Lu Xugang
 * @date 2020/12/24 15:08
 */
public class MergeDuringRefreshTest {
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
        String sortedField = "sortByNumber";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
        conf.setUseCompoundFile(false);
        conf.setMaxCommitMergeWaitMillis(1000);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Document doc ;
        int count = 0;
        while (count++ < 140000){
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
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", type));
            doc.add(new NumericDocValuesField(sortedField, 2L));
            indexWriter.addDocument(doc);
            if(count == 70000){
                indexWriter.commit();
            }
        }
        indexWriter.commit();
    }

    public static void main(String[] args) throws Exception{
        MergeDuringRefreshTest test = new MergeDuringRefreshTest();
        test.doIndexAndSearch();
    }
}
