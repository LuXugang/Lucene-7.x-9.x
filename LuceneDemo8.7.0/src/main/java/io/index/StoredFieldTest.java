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
 * @date 2020/11/5 1:55 PM
 */
public class StoredFieldTest {
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
        conf.setMergeScheduler(new SerialMergeScheduler());
        String sortedField = "oldSorterRule";
        String sortedField2 = "newSorterRule";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
//        SortField indexSortField = new SortField(sortedField2, SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
//        conf.setIndexSort(indexSort);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        Document doc ;
        int count = 0;
        while (count++ < 10000000){
//            doc = new Document();
//            doc.add(new Field("good", "the name is name", type));
//            indexWriter.addDocument(doc);
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "abc", type));
            doc.add(new Field("content", "cd", type));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
            doc.add(new NumericDocValuesField(sortedField, 3));
            doc.add(new NumericDocValuesField(sortedField2, 1));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", type));
            doc.add(new NumericDocValuesField(sortedField, 2));
            indexWriter.addDocument(doc);
            if(count % 341 == 0){
                indexWriter.commit();
//                break;
            }
        }
        indexWriter.commit();
        System.out.println("ab");
    }

    public static void main(String[] args) throws Exception{
        StoredFieldTest test = new StoredFieldTest();
        test.doIndexAndSearch();
    }
}
