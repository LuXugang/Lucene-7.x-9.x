package io.codecs;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/5/13 11:23 上午
 */
public class IndexedDISITest {
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

        Random random = new Random();
        Document doc = new Document();
        int count = 0;
        while (count < 200000) {
            doc = new Document();
            doc.add(new NumericDocValuesField("age", random.nextInt(100)));
            doc.add(new TextField("abc", "document2", Field.Store.YES));
            indexWriter.addDocument(doc);
            count++;

            if(count == 32 || count == 23 || count == 35){
                doc = new Document();
                doc.add(new NumericDocValuesField("level", random.nextInt(100)));
                doc.add(new TextField("abc", "document1", Field.Store.YES));
                indexWriter.addDocument(doc);
                count++;
            }

            if(count == 131080|| count == 131081){
                doc = new Document();
                doc.add(new NumericDocValuesField("level", random.nextInt(100)));
                doc.add(new TextField("abc", "document1", Field.Store.YES));
                indexWriter.addDocument(doc);
                count++;
            }
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("abc", "document1")), BooleanClause.Occur.MUST);
        Sort sortByLevel = new Sort(new SortField("level", SortField.Type.INT, true));
        TopDocs docs2 = searcher.search(builder.build(), 1000 , sortByLevel);


        System.out.println("sort by level");
        for (ScoreDoc scoreDoc: docs2.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }
    }

    public static void main(String[] args) throws Exception{
        IndexedDISITest test = new IndexedDISITest();
        test.doIndexAndSearch();
    }
}
