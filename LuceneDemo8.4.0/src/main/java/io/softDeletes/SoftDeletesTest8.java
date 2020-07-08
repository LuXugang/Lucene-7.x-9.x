package io.softDeletes;

import io.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * @author Lu Xugang
 * @date 2020/7/7 3:23 下午
 */
public class SoftDeletesTest8 {
    private Directory directory;
    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 放在方法外 这个变量能高亮显示
    private IndexWriter indexWriter;
    IndexWriterConfig indexWriterConfig;
    public void doIndexAndSearch() throws Exception {
        indexWriterConfig = new IndexWriterConfig(new WhitespaceAnalyzer());
        String softDeleteField = "softDeleteField";
        indexWriterConfig.setSoftDeletesField(softDeleteField);
        indexWriterConfig.setUseCompoundFile(false);
        Supplier<Query> querySupplier = () ->new TermQuery(new Term("sex","male"));
        indexWriterConfig.setMergePolicy(new SoftDeletesRetentionMergePolicy(indexWriterConfig.getSoftDeletesField(), querySupplier,new TieredMergePolicy()));
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        Document doc;
        // 第一个段：文档0
        doc = new Document();
        doc.add(new StringField("author", "D0", Field.Store.YES));
        doc.add(new StringField("sex", "female", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesFiled", 2));
        indexWriter.addDocument(doc);
        // 第一个段：文档1
        doc = new Document();
        doc.add(new StringField("author", "D0", Field.Store.YES));
        doc.add(new StringField("sex", "male", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesFiled", 3));
        indexWriter.addDocument(doc);
        // 第一个段：文档2
        doc = new Document();
        doc.add(new StringField("author", "D1", Field.Store.YES));
        doc.add(new StringField("sex", "male", Field.Store.YES));
        doc.add(new NumericDocValuesField(softDeleteField, 3));
        indexWriter.addDocument(doc);
        // 第一个段：文档3
        doc = new Document();
        doc.add(new StringField("author", "D2", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 第一个段：文档4
        Document newDoc = new Document();
        newDoc.add(new StringField("author", "D4", Field.Store.YES));
        indexWriter.softUpdateDocument(new Term("author", "D0"), newDoc, new NumericDocValuesField(softDeleteField, 3));
        indexWriter.commit();
        // 第二个段：文档0
        doc = new Document();
        doc.add(new StringField("author", "D7", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        assert reader.numDocs() == 3;
        assert reader.maxDoc() == 5;
        assert reader.document(0).get("author").equals("D0");
        assert reader.document(0).get("sex").equals("male");

        assert reader.document(1).get("author").equals("D1");
        assert reader.document(2).get("author").equals("D2");
        assert reader.document(3).get("author").equals("D4");
        assert reader.document(4).get("author").equals("D7");
    }

    public static void main(String[] args) throws Exception{
        SoftDeletesTest9 test = new SoftDeletesTest9();
        test.doIndexAndSearch();
    }
}
