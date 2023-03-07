import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TermOrdValComparatorTest {
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
        doc.add(new StringField("name", "a", Field.Store.YES));
        doc.add(new SortedDocValuesField("name", new BytesRef("b")));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new StringField("name", "a", Field.Store.YES));
        doc.add(new SortedDocValuesField("name", new BytesRef("a")));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new StringField("name", "a", Field.Store.YES));
        doc.add(new SortedDocValuesField("name", new BytesRef("c")));
        indexWriter.addDocument(doc);

        int count = 0;
        while (count++ < 10000){
            doc = new Document();
            doc.add(new StringField("name", "a", Field.Store.YES));
            doc.add(new SortedDocValuesField("name", new BytesRef("d" + new Random().nextInt())));
            indexWriter.addDocument(doc);
        }

        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        System.out.println("document count : "+reader.maxDoc()+"");


        Query query = new TermQuery(new Term("name", "a"));
        Sort searchSort = new Sort(new SortField("name", SortField.Type.STRING));
        // 返回Top5的结果
        int resultTopN = 1301;

        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN, searchSort).scoreDocs;

        System.out.println("Total Result Number: "+scoreDocs.length+"");
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            // 输出满足查询条件的 文档号
            System.out.println("result"+i+": 文档"+scoreDoc.doc+"");
        }
    }
    public static void main(String[] args) throws Exception{
        TermOrdValComparatorTest test = new TermOrdValComparatorTest();
        test.doSearch();
    }
}
