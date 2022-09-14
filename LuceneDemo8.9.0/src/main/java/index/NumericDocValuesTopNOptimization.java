package index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.FileOperation;

/**
 * @author Lu Xugang
 * @date 2021/6/18 10:54 上午 文章中的demo，不要修改
 */
public class NumericDocValuesTopNOptimization {
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

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        int count = 0;

        Random random = new Random();
        boolean optimize = random.nextBoolean();
        System.out.println(optimize ? "enableOptimization" : "disableOptimization");

        Document doc;
        int sortValue;
        while (count++ < 1000000) {
            sortValue = random.nextInt(100);
            doc = new Document();
            doc.add(new StringField("content", "whatever", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortField", sortValue));
            doc.add(new IntPoint("sortField", sortValue));
            indexWriter.addDocument(doc);
        }

        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        SortField sortField = new SortedNumericSortField("sortField", SortField.Type.INT);
        if (optimize) {
            // 开启优化
            sortField.setCanUsePoints();
        }
        Sort sort = new Sort(sortField);
        int topN = 3;
        TopFieldCollector collector = TopFieldCollector.create(sort, topN, topN);
        searcher.search(new MatchAllDocsQuery(), collector);
        System.out.println("收集器处理的文档数量: " + collector.getTotalHits() + "");
    }

    public static void main(String[] args) throws Exception {
        NumericDocValuesTopNOptimization test = new NumericDocValuesTopNOptimization();
        test.doSearch();
    }
}
