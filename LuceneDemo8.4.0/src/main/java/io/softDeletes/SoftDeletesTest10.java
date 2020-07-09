package io.softDeletes;

import io.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.Supplier;

/**
 * @author Lu Xugang
 * @date 2020/7/9 5:22 下午
 */
public class SoftDeletesTest10 {
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
        indexWriterConfig.setUseCompoundFile(false);
        boolean useSoftMergePolice = new Random().nextBoolean();
        if(useSoftMergePolice){
            System.out.println("softMergePolice enable");
            Supplier<Query> querySupplier = MatchAllDocsQuery::new;
            indexWriterConfig.setMergePolicy(new SoftDeletesRetentionMergePolicy("balabala", querySupplier,new TieredMergePolicy()));
        }else {
            System.out.println("softMergePolice disable");
            indexWriterConfig.setMergePolicy(NoMergePolicy.INSTANCE);
        }
        if(indexWriterConfig.getSoftDeletesField() == null){
            System.out.println("softDeletesConfig disable");
        }
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new StringField("author", "D0", Field.Store.YES));
        doc.add(new StringField("sex", "female", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 文档1
        Document newDoc = new Document();
        newDoc.add(new StringField("author", "D4", Field.Store.YES));
        indexWriter.addDocument(newDoc);
        indexWriter.deleteDocuments(new Term("author", "D0"));
        indexWriter.deleteDocuments(new Term("author", "D4"));
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        if(useSoftMergePolice){
            assert reader.numDocs() == 0;
            assert reader.maxDoc() == 2;
        }else {
            assert reader.numDocs() == 0;
            assert reader.maxDoc() == 0;
        }
        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception{
        SoftDeletesTest10  test = new SoftDeletesTest10();
        test.doIndexAndSearch();
    }
}
