package lucene.softDeletes;

import io.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/6/21 7:55 下午
 */
public class SoftDeletesTest3 {
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
        boolean update = (new Random()).nextBoolean();
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new StringField("abc", "document0", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesField", 5));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new StringField("abc", "document1", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesField", 6));
        indexWriter.addDocument(doc);
        // 文档2
        Document newDoc = new Document();
        newDoc.add(new StringField("abc", "document2", Field.Store.YES));
        newDoc.add(new NumericDocValuesField("docValuesField", 7));
        if(update){
            indexWriter.softUpdateDocument(new Term("abc", "document1"), newDoc, new NumericDocValuesField("docValuesField", 3));
        }else {
            indexWriter.addDocument(newDoc);
        }
        indexWriter.commit();
        DirectoryReader readerBeforeMerge = DirectoryReader.open(indexWriter);
        Sort sort = new Sort(new SortField("docValuesField", SortField.Type.LONG));
        ScoreDoc[] scoreDocs = (new IndexSearcher(readerBeforeMerge)).search(new MatchAllDocsQuery(), 100, sort).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("docId: 文档" + scoreDoc.doc + ", FieldValue of Field abc: " + readerBeforeMerge.document(scoreDoc.doc).get("abc") + "");
        }
    }

    public static void main(String[] args) throws Exception{
        SoftDeletesTest3 test = new SoftDeletesTest3();
        test.doIndexAndSearch();
    }
}
