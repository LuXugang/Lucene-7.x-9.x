package lucene.docValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Field;
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
 * @date 2019-04-12 00:15
 */
public class BinaryDocValuesFieldTest {
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

        String fieldName = "superStar";
        Document doc ;
        // 0
        doc = new Document();
        doc.add(new BinaryDocValuesField(fieldName, new BytesRef("c")));
        doc.add(new TextField("superStar", "Andy", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new BinaryDocValuesField(fieldName, new BytesRef("b")));
        doc.add(new TextField("superStar", "Eason", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new BinaryDocValuesField(fieldName, new BytesRef("d")));
        doc.add(new TextField("superStar", "Jay", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 3
        doc = new Document();
        doc.add(new BinaryDocValuesField(fieldName, new BytesRef("e")));
        doc.add(new TextField("superStar", "Jolin", Field.Store.YES));
        indexWriter.addDocument(doc);
        // 4
        doc = new Document();
        doc.add(new BinaryDocValuesField(fieldName, new BytesRef("a")));
        doc.add(new TextField("superStar", "KUN", Field.Store.YES));
        indexWriter.addDocument(doc);
        indexWriter.commit();

        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        Sort sort = new Sort(new SortField(fieldName, SortField.Type.STRING_VAL));
        TopDocs docs = searcher.search(new MatchAllDocsQuery(), 3 , sort);

        for (ScoreDoc scoreDoc: docs.scoreDocs){
            Document document = searcher.doc(scoreDoc.doc);
            System.out.println("name is "+ document.get("abc")+"");
        }

    }




    public static int getLength(){
        Random random = new Random();
        int length = random.nextInt(5);
        if (length < 3){
            length = length + 3;
        }
        return length;
    }

    public static void main(String[] args) throws Exception{
//    BytesRef ref1 = new BytesRef("abcdef");
//    BytesRef ref2 = new BytesRef("abdefd");
//    int ssortKeyLength = StringHelper.sortKeyLength(ref1, ref2);
//    System.out.println(ssortKeyLength);
        BinaryDocValuesFieldTest test = new BinaryDocValuesFieldTest();
        test.doSearch();
    }
}
