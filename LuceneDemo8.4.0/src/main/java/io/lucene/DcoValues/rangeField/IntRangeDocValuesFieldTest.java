package io.lucene.DcoValues.rangeField;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRangeDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/7/23 12:17 上午
 */
public class IntRangeDocValuesFieldTest {
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
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        final int[] lowRange = {0, 4};
        final int[] highRange = {2, 8};
        Query query = IntRangeDocValuesField.newSlowIntersectsQuery("level", lowRange, highRange);
        TopDocs docs2 = searcher.search(query, 1000);
    }

    public static void main(String[] args) throws Exception{
        IntRangeDocValuesFieldTest test = new IntRangeDocValuesFieldTest();
        test.doIndexAndSearch();
    }
}
