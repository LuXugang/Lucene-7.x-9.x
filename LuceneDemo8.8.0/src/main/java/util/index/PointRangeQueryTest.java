package util.index;

import jdk.nashorn.internal.codegen.types.Type;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2021/6/11 11:00 上午
 */
public class PointRangeQueryTest {
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

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new IntPoint("sortField", 1));
        doc.add(new NumericDocValuesField("sortField", 1));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new IntPoint("sortField", 2));
        doc.add(new NumericDocValuesField("sortField", 2));
        indexWriter.addDocument(doc);
//        // 文档2
//        doc = new Document();
//        doc.add(new TextField("abc", "edd", Field.Store.YES ));
//        indexWriter.addDocument(doc);
        int count = 0 ;
        int a;
        while (count++ < 40960){
            doc = new Document();
            a = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            doc.add(new IntPoint("sortField", a));
            doc.add(new NumericDocValuesField("sortField", a));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        int [] lowValue = {-1};
        int [] upValue = {70};
        Query query = IntPoint.newRangeQuery("sortField", lowValue, upValue);


        // 返回Top5的结果
        int resultTopN = 514;


        SortField sortField = new SortedNumericSortField("sortField", SortField.Type.INT);
        sortField.setCanUsePoints();
        Sort sort = new Sort(sortField);
        TopFieldCollector collector = TopFieldCollector.create(sort, resultTopN, 520);
        searcher.search(query, collector);
        ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
        for (ScoreDoc scoreDoc : scoreDocs) {
            System.out.println("文档号: "+scoreDoc.doc+"");
        }

        boolean isEarlyTerminal = collector.isEarlyTerminated();
        System.out.println(isEarlyTerminal ? "early exit" : "not early exit");
        System.out.println("totalHits: "+collector.getTotalHits()+"");

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        PointRangeQueryTest test = new PointRangeQueryTest();
        test.doSearch();
    }
}
