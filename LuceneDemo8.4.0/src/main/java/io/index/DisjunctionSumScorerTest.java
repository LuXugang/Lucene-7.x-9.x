package io.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
 * @date 2020/9/14 1:26 下午
 */
public class DisjunctionSumScorerTest {

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

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        int count = 0;
        Document doc ;
        while (count++ < 10000){
            doc = new Document();
            doc.add(new TextField("author",  "ably lily baby andy lucy ably", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new TextField("author",  "lily and tom for you", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new TextField("author",  "i love you good", Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("author", "lily")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("author", "lucy")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("author", "good")), BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(2);
        searcher.search(builder.build(), 3);

        System.out.println("abc");

    }

    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyz";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(26);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }


    public static void main(String[] args) throws Exception{
        DisjunctionSumScorerTest test = new DisjunctionSumScorerTest();
        test.doSearch();
    }
}
