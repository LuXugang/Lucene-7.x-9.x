package io.softDeletes;

import io.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/7/1 3:36 下午
 */
public class SoftDeletesTest7 {
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
        indexWriterConfig.setSoftDeletesField("softDeleteField");
        indexWriterConfig.setUseCompoundFile(false);
        indexWriterConfig.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new StringField("abc", "D1", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesFiled", 2));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new StringField("abc", "D3", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesFiled", 3));
        doc.add(new NumericDocValuesField("softDeleteField", 3));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new StringField("abc", "D2", Field.Store.YES));
        doc.add(new NumericDocValuesField("softDeleteField", 3));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new StringField("abc", "document4", Field.Store.YES));
        doc.add(new NumericDocValuesField("softDeleteField", 3));
        indexWriter.addDocument(doc);
        // 第一次软删除
        Document newDoc = new Document();
        newDoc.add(new StringField("abc", "document3", Field.Store.YES));
        indexWriter.updateDocValues(new Term("abc", "document3"), new NumericDocValuesField("softDeleteField", 3));
//
        indexWriter.commit();
        DirectoryReader readerBeforeMerge = DirectoryReader.open(indexWriter);
        ScoreDoc[] scoreDocs = (new IndexSearcher(readerBeforeMerge)).search(new MatchAllDocsQuery(), 100).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("docId: 文档" + scoreDoc.doc + ", FieldValue of Field abc: " + readerBeforeMerge.document(scoreDoc.doc).get("abc") + "");
        }
    }

    public static void main(String[] args) throws Exception{
        SoftDeletesTest7 test = new SoftDeletesTest7();
        test.doIndexAndSearch();
    }
}
