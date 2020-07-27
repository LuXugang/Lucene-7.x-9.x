package io.lucene.DcoValues.rangeField;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.document.IntRangeDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/7/23 4:32 下午
 */
public class IntRangeTest {
    private Directory directory;
    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private final Analyzer analyzer = new WhitespaceAnalyzer();
    private final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, conf);
        Document doc;
        // 文档0
        doc = new Document();
        int[] min1 = {1, 1};
        int[] max1 = {4, 4};
        // 第一个RangeField
        doc.add(new IntRange("level", min1, max1));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        int[] min2 = {3, 2};
        int[] max2 = {6, 5};
        // 第二个RangeField
        doc.add(new IntRange("level", min2, max2));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        int[] min3 = {8, 8};
        int[] max3 = {10, 10};
        // 第三个RangeField
        doc.add(new IntRange("level", min3, max3));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        int[] min4 = {9, 6};
        int[] max4 = {10, 7};
        // 第四个RangeField
        doc.add(new IntRange("level", min4, max4));
        indexWriter.addDocument(doc);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        final int[] lowQueryRange = {2, 0};
        final int[] highQueryRange = {8, 8};
        Query query = IntRange.newIntersectsQuery("level", lowQueryRange, highQueryRange);
        ScoreDoc[] scoreDocs = searcher.search(query, 1000).scoreDocs;
        assert scoreDocs.length == 3;
        assert scoreDocs[0].doc == 0;
        assert scoreDocs[1].doc == 1;
        assert scoreDocs[2].doc == 2;
    }

    public static void main(String[] args) throws Exception{
        IntRangeTest test = new IntRangeTest();
        test.doIndexAndSearch();
    }
}
