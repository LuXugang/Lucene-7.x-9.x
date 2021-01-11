package io.index;

import io.util.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

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
        conf.setReaderPooling(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        conf.setReaderPooling(true);
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
        FieldType type1 = new FieldType();
        type1.setStored(true);
        type1.setTokenized(true);
        type1.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        Document doc ;
        int count = 0;
        while (count++ < 3800){
//            doc = new Document();
//            doc.add(new Field("good", "the name is name", type));
//            indexWriter.addDocument(doc);
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "abc", type1));
            doc.add(new Field("content", "cd", type1));
            doc.add(new IntPoint("intField", 3, 4));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new Field("content", "cd", type1));
            doc.add(new Field("attachment", "cd", type));
            doc.add(new NumericDocValuesField(sortedField, 3));
            doc.add(new NumericDocValuesField(sortedField2, 1));
            doc.add(new IntPoint("intField", 7, 4));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", type1));
            doc.add(new NumericDocValuesField(sortedField, 2));
            doc.add(new IntPoint("intField", new Random().nextInt(100), 4));
            indexWriter.addDocument(doc);
            if(count == 3751){
                indexWriter.deleteDocuments(new TermQuery(new Term("content", new BytesRef("cd"))));
                indexWriter.updateNumericDocValue(new Term("attachment", new BytesRef("cd")), sortedField, 100);
            }

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
