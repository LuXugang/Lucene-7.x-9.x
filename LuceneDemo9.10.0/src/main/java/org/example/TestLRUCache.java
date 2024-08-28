package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollectorManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/*文章中的demo，不要修改*/
public class TestLRUCache {

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
        final SortField sortField = new SortField("my_field", SortField.Type.LONG);
        sortField.setMissingValue(0L); // set a competitive missing value
        final Sort sort = new Sort(sortField);
        // 索引数据使用排序
        conf.setIndexSort(sort);
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        final int numDocs =500000;
        for (int i = 0; i < numDocs; ++i) {
            final Document doc = new Document();
            if ((i % 500) != 0) { // miss values on every 500th document
                doc.add(new NumericDocValuesField("my_field", i));
                doc.add(new LongPoint("my_field", i));
            }
            doc.add(new TextField("a",  "i", Field.Store.YES));
            doc.add(new TextField("b",  "i", Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        final IndexReader reader = DirectoryReader.open(indexWriter);
        indexWriter.forceMerge(1);
        indexWriter.close();
        IndexSearcher searcher = new IndexSearcher(reader);
        final int numHits = 3;
        final int totalHitsThreshold = 3;

        {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new TermQuery(new Term("a", "i")), BooleanClause.Occur.MUST);
            builder.add(new TermQuery(new Term("b", "i")), BooleanClause.Occur.MUST);
            int i = 10;
            TopDocs topDocs;
            for (int i1 = 0; i1 < i; i1++) {
                System.out.println("i1 = " + i1);
                // 当i1=3时，会缓存这次查询
                // 查询使用跟索引时相同的排序规则
                final TopFieldCollectorManager collectorManager =
                        new TopFieldCollectorManager(sort, numHits, totalHitsThreshold);
                topDocs = searcher.search(builder.build(), collectorManager);
            }
        }
    }

    public static void main(String[] args) throws Exception{
        TestLRUCache test = new TestLRUCache();
        test.doSearch();
    }
}
