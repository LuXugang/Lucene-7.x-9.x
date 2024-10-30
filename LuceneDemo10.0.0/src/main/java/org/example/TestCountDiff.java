package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;


public class TestCountDiff {
    private Directory directory;

    {
        try {
            deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doIndex() throws Exception {
        conf.setUseCompoundFile(false);
//        SortField sortField = new SortedNumericSortField("content", SortField.Type.INT);
        SortField sortField = new SortedSetSortField("content", false);
        Sort sort = new Sort(sortField);
        conf.setIndexSort(sort);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        Document doc;
        int count = 0 ;
        while (count < (4096 * 100)){
            doc = new Document();
            int a = random.nextInt(12312312);
//            a = a < 2 ? a + 10 : a;
//            a = a > 90 ? a - 2 : a;
//            if(count++ == 10){
//                doc.add(new IntPoint("padingvalue", a));
//                indexWriter.addDocument(doc);
//                continue;
//            }
            doc.add(new IntPoint("content", count));
//            doc.add(SortedNumericDocValuesField.indexedField("content", count));
            doc.add(SortedSetDocValuesField.indexedField("content", new BytesRef(String.valueOf(count))));
            doc.add(new StringField("content", "a", org.apache.lucene.document.Field.Store.YES));
            count ++;
            indexWriter.addDocument(doc);
        }

        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader r = DirectoryReader.open(indexWriter);
        IndexSearcher s = new IndexSearcher(r);

        int  lowValue = 3;
        int  upValue = 25;
//
//    int [] lowValue = {-1, -1};
//    int [] upValue = {100, 100};

//        int num;
//        num = s.count(IntField.newRangeQuery("content", lowValue, upValue));
//        System.out.println("result number : "+ num +"");
        Query query = new TermQuery(new Term("content", "a"));
        query = SortedSetDocValuesField.newSlowRangeQuery("content", new BytesRef("1"), new BytesRef("8200"),true, true);
        TopFieldDocs docs = s.search(query, 10, sort);
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            System.out.println(scoreDoc.doc);
        }

        // Per-top-reader state:k
    }



    public static void main(String[] args) throws Exception{
        TestCountDiff query = new TestCountDiff();
        query.doIndex();
    }

    public static void deleteFile(String filePath) {
        File dir = new File(filePath);
        if (dir.exists()) {
            File[] tmp = dir.listFiles();
            assert tmp != null;
            for (File aTmp : tmp) {
                if (aTmp.isDirectory()) {
                    deleteFile(filePath + "/" + aTmp.getName());
                } else {
                    aTmp.delete();
                }
            }
            dir.delete();
        }
    }
}
