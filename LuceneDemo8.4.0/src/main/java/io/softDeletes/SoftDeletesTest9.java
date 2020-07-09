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
 * @date 2020/7/8 1:37 下午
 */
public class SoftDeletesTest9 {
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
        boolean useSoftMergePolice = new Random().nextBoolean();
        if(useSoftMergePolice){
            System.out.println("softMergePolice enable");
            Supplier<Query> querySupplier = MatchAllDocsQuery::new;
            indexWriterConfig.setMergePolicy(new SoftDeletesRetentionMergePolicy(indexWriterConfig.getSoftDeletesField(), querySupplier,new TieredMergePolicy()));
        }else {
            System.out.println("softMergePolice disable");
            indexWriterConfig.setMergePolicy(NoMergePolicy.INSTANCE);
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
        indexWriter.softUpdateDocument(new Term("author", "D0"), newDoc, new NumericDocValuesField(softDeleteField, 3));
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
        SoftDeletesTest9 test = new SoftDeletesTest9();
        test.doIndexAndSearch();
    }
}
