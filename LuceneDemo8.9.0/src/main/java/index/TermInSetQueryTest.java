package index;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

public class TermInSetQueryTest {
    //    private static String alphabet = "a b c d e f g ";
    private static String alphabet = "a b c d e f g h i j k l m n o p q r s t";
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
        Document doc;
        int count = 0;
        while (count++ < 409600) {
            // 文档0
            doc = new Document();
            doc.add(new Field("body", "china a ", fieldType));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new Field("body", "china b", fieldType));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new Field("body", "china d", fieldType));
            indexWriter.addDocument(doc);
        }

        indexWriter.commit();

        DirectoryReader directoryReader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(directoryReader);

        List<BytesRef> terms = new ArrayList<>();
        for (String s : alphabet.split(" ")) {
            terms.add(new BytesRef(s.trim()));
        }
        Query query = new TermInSetQuery("body", terms);

        int queryCount = 100;
        for (int i = 0; i < queryCount; i++) {
            ScoreDoc[] result = searcher.search(query, 100).scoreDocs;
            for (ScoreDoc scoreDoc : result) {
                System.out.println("文档号: " + scoreDoc.doc + " 文档分数: " + scoreDoc.score + "");
            }
        }
        System.out.println("DONE");
    }

    public static void main(String[] args) throws Exception {
        TermInSetQueryTest test = new TermInSetQueryTest();
        test.doSearch();
    }
}
