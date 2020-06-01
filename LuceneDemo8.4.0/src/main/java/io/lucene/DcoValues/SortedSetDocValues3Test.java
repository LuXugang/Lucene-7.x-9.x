package io.lucene.DcoValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
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
 * @date 2020/6/1 9:29 下午
 */
public class SortedSetDocValues3Test {
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

    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        int count = 0;
        Document doc;
        while (count++ < 20000) {
            // 文档0
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef(getRandomString(random.nextInt(25)))));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("star")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("of")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档3
            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("month")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new SortedDocValuesField("level", new BytesRef("month")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);

            // 后面还有很多...
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


//        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MIN));
//        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MIDDLE_MIN));

        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MIDDLE_MAX));

//        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MAX));


        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("abc", "document1")), BooleanClause.Occur.MUST);
        TopDocs docs2 = searcher.search(builder.build(), 1000 , sortByLevel);


        System.out.println("sort by level");
        for (ScoreDoc scoreDoc: docs2.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }
    }

    public static void main(String[] args) throws Exception{
        SortedSetDocValues3Test test = new SortedSetDocValues3Test();
        test.doIndexAndSearch();
    }
}
