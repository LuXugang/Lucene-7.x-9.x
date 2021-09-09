package query;

import index.DisjunctionMaxQueryTest;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LRUCacheTest {
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
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        int count = 0 ;
        int a;
        while (count < 40960){
            doc = new Document();
            a = random.nextInt(100);
            a = a <= 2 ? a + 4 : a;
            doc.add(new IntPoint("number", a));
            doc.add(new NumericDocValuesField("number", a));
            doc.add(new BinaryDocValuesField("numberString", new BytesRef(String.valueOf(a))));
            if(count % 2 == 0){
                doc.add(new Field("content", "my good teach", fieldType));
            }else {
                doc.add(new Field("content", "my efds", fieldType));
            }
            doc.add(new Field("content", "ddf", fieldType));
            indexWriter.addDocument(doc);
            count++;
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Query query1 = new TermQuery(new Term("content", new BytesRef("my")));
        Query query2 = new TermQuery(new Term("content", new BytesRef("efds")));
        builder.add(query1, BooleanClause.Occur.MUST);
        builder.add(query2, BooleanClause.Occur.MUST);

        Collector collector = new TotalHitCountCollector();

        // 返回Top5的结果
        int resultTopN = 100;
        searcher.search(builder.build(), collector);

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        LRUCacheTest test = new LRUCacheTest();
        test.doSearch();
    }
}
