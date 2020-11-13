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
 * @date 2020/10/29 10:00 AM
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
        String sortedField = "sortByNumber";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
        conf.setIndexSort(indexSort);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Document doc ;
        int count = 0;
        while (count++ < 100){
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
        Document document;
        List<IndexableField> fields;
        IndexReader reader = DirectoryReader.open(indexWriter);
        // 测试第一个段中的文档0（段内文档号）
        document = reader.document(0);
        // 文档0有3个存储域
        fields = document.getFields();
        assert fields.size() == 3;
        assert fields.get(0).name().equals("content");
        assert fields.get(1).name().equals("content");
        assert fields.get(2).name().equals("author");

        // 测试第一个段中的文档1（段内文档号）
        document = reader.document(1);
        // 文档1有0个存储域
        fields = document.getFields();
        assert fields.size() == 0;

        // 测试第一个段中的文档2（段内文档号）
        document = reader.document(2);
        // 文档2有1个存储域
        fields = document.getFields();
        assert fields.size() == 1;
        assert fields.get(0).name().equals("content");

        // 测试第二个段中的第一篇文档, 第二个段中的第一篇文档的全局文档号为文档3, 跟第一个段中的第一篇文档具有相同的document信息
        document = reader.document(3);
        // 文档0有3个存储域
        fields = document.getFields();
        assert fields.size() == 3;
        assert fields.get(0).name().equals("content");
        assert fields.get(1).name().equals("content");
        assert fields.get(2).name().equals("author");
    }

    public static void main(String[] args) throws Exception{
        StoredFieldTest test = new StoredFieldTest();
        test.doIndexAndSearch();
    }
}
