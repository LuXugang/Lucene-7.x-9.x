package io.softDeletes;

import io.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Lu Xugang
 * @date 2020/6/15 11:36 下午
 */
public class HistoryRetention {
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
        String softDeletesField  = "softDeleteField";
        indexWriterConfig.setSoftDeletesField(softDeletesField);
        indexWriterConfig.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        Instant now = Instant.now();
        Instant time24HoursAgo = now.minus(Duration.ofDays(1));
        Supplier<Query> docsOfLast24Hours = () -> LongPoint.newRangeQuery("creation_date", time24HoursAgo.toEpochMilli(), now.toEpochMilli());
        indexWriterConfig.setMergePolicy(new SoftDeletesRetentionMergePolicy(softDeletesField, docsOfLast24Hours,
                new LogDocMergePolicy()));
        long time28HoursAgo = now.minus(Duration.ofHours(28)).toEpochMilli();
        boolean useSoftDelete = new Random().nextBoolean();
        Document doc = new Document();
        doc.add(new StringField("id", "1", Field.Store.YES));
        doc.add(new StringField("version", "1", Field.Store.YES));
        doc.add(new LongPoint("creation_date", time28HoursAgo));
        indexWriter.addDocument(doc);
        indexWriter.flush();

        long time26HoursAgo = now.minus(Duration.ofHours(26)).toEpochMilli();
        doc = new Document();
        doc.add(new StringField("id", "1", Field.Store.YES));
        doc.add(new StringField("version", "2", Field.Store.YES));
        doc.add(new LongPoint("creation_date", time26HoursAgo));
        if(useSoftDelete){
            indexWriter.softUpdateDocument(new Term("id", "1"), doc, new NumericDocValuesField(softDeletesField, 1));
        }else {
            indexWriter.updateDocument(new Term("id", "1"), doc);
        }
        indexWriter.flush();

        long time23HoursAgo = now.minus(Duration.ofHours(23)).toEpochMilli();
        doc = new Document();
        doc.add(new StringField("id", "1", Field.Store.YES));
        doc.add(new StringField("version", "3", Field.Store.YES));
        doc.add(new LongPoint("creation_date", time23HoursAgo));
        if(useSoftDelete){
            indexWriter.softUpdateDocument(new Term("id", "1"), doc, new NumericDocValuesField(softDeletesField, 1));
        }else {
            indexWriter.updateDocument(new Term("id", "1"), doc);
        }
        indexWriter.flush();

        long time12HoursAgo = now.minus(Duration.ofHours(12)).toEpochMilli();
        doc = new Document();
        doc.add(new StringField("id", "1", Field.Store.YES));
        doc.add(new StringField("version", "4", Field.Store.YES));
        doc.add(new LongPoint("creation_date", time12HoursAgo));
        if(useSoftDelete){
            indexWriter.softUpdateDocument(new Term("id", "1"), doc, new NumericDocValuesField(softDeletesField, 1));
        }else {
            indexWriter.updateDocument(new Term("id", "1"), doc);
        }
        indexWriter.flush();

        doc = new Document();
        doc.add(new StringField("id", "1", Field.Store.YES));
        doc.add(new StringField("version", "5", Field.Store.YES));
        doc.add(new LongPoint("creation_date", now.toEpochMilli()));
        if(useSoftDelete){
            indexWriter.softUpdateDocument(new Term("id", "1"), doc, new NumericDocValuesField(softDeletesField, 1));
        }else {
            indexWriter.updateDocument(new Term("id", "1"), doc);
        }
        indexWriter.flush();

        indexWriter.forceMerge(1);
        Set<String> versions;
        try (DirectoryReader reader = DirectoryReader.open(indexWriter)) {
            if(useSoftDelete){
                System.out.println("softDelete");
                assert reader.numDocs() == 1;
                assert reader.maxDoc() == 3;
                versions = new HashSet<>();
                versions.add(reader.document(0, Collections.singleton("version")).get("version"));
                versions.add(reader.document(1, Collections.singleton("version")).get("version"));
                versions.add(reader.document(2, Collections.singleton("version")).get("version"));
                assert versions.contains("5");
                assert versions.contains("4");
                assert versions.contains("3");
            }else {
                System.out.println("hardDelete");
                assert reader.numDocs() == 1;
                assert reader.maxDoc() == 1;
                versions = new HashSet<>();
                versions.add(reader.document(0, Collections.singleton("version")).get("version"));
                assert versions.contains("5");
            }

        }
    }

    public static void main(String[] args) throws Exception{
        HistoryRetention test = new HistoryRetention();
        test.doIndexAndSearch();
    }
}
