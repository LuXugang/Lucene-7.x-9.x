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
import java.util.Random;
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
        indexWriterConfig.setSoftDeletesField("softDeleteField");
        indexWriterConfig.setUseCompoundFile(false);
//        boolean useSoftMergePolice = new Random().nextBoolean();
        boolean useSoftMergePolice  = true;
        if(!useSoftMergePolice){
            System.out.println("unUseSoftMergePolice");
            indexWriterConfig.setMergePolicy(new TieredMergePolicy());
        }else {
            System.out.println("useSoftMergePolice");
            Supplier<Query> querySupplier = () -> new TermQuery(new Term("abc", "D3"));
            indexWriterConfig.setMergePolicy(new SoftDeletesRetentionMergePolicy(indexWriterConfig.getSoftDeletesField(), querySupplier,new TieredMergePolicy()));
        }
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        Document doc;
        // 文档
        doc = new Document();
        doc.add(new StringField("abc", "D0", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesFiled", 2));
        indexWriter.addDocument(doc);
        // 文档
        doc = new Document();
        doc.add(new StringField("abc", "D1", Field.Store.YES));
        doc.add(new NumericDocValuesField("docValuesFiled", 3));
        indexWriter.addDocument(doc);
        // 文档
        doc = new Document();
        doc.add(new StringField("abc", "D2", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档
        Document newDoc = new Document();
        newDoc.add(new StringField("abc", "D4", Field.Store.YES));
        indexWriter.softUpdateDocument(new Term("abc", "D0"), newDoc, new NumericDocValuesField("softDeleteField", 3));
        indexWriter.deleteDocuments(new Term("abc", "D1"));
        indexWriter.commit();
        // 文档
        doc = new Document();
        doc.add(new StringField("abc", "D7", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader readerBeforeMerge = DirectoryReader.open(indexWriter);
        ScoreDoc[] scoreDocs = (new IndexSearcher(readerBeforeMerge)).search(new MatchAllDocsQuery(), 100).scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("docId: 文档" + scoreDoc.doc + ", FieldValue of Field abc: " + readerBeforeMerge.document(scoreDoc.doc).get("abc") + "");
        }
    }

    public static void main(String[] args) throws Exception{
        SoftDeletesTest8 test = new SoftDeletesTest8();
        test.doIndexAndSearch();
    }
}
