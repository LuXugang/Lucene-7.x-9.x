package io.lucene.DcoValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/7/15 10:02 上午
 */
public class BinaryDocValues1Test {
    private Directory directory;
    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, conf);

        Document doc;
        // 文档0
        doc = new Document();
        int[] min1 = {1, 3};
        int[] max1 = {2, 4};
        doc.add(new IntRangeDocValuesField("level", min1, max1));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        int[] min2 = {3, 5};
        int[] max2 = {4, 6};
        doc.add(new IntRangeDocValuesField("level", min2, max2));
        indexWriter.addDocument(doc);
        final int[] lowRange = {0, 4};
        final int[] highRange = {2, 8};
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = IntRangeDocValuesField.newSlowIntersectsQuery("level", lowRange, highRange);
        TopDocs docs2 = searcher.search(query, 1000 );

        for (ScoreDoc scoreDoc: docs2.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }
    }

    public static void main(String[] args) throws Exception{
        BinaryDocValues1Test test = new BinaryDocValues1Test();
        test.doIndexAndSearch();
    }
}
