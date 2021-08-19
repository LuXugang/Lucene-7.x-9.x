package index;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UpdateDocumentTest {
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

    public void doSearch() throws Exception {
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Document doc;

        // 文档0
        doc = new Document();
        doc.add(new Field("title", "a", fieldType));
        doc.add(new Field("order", "第一个添加的文档", fieldType));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        // 文档1
        doc = new Document();
        doc.add(new Field("title", "b", fieldType));
        doc.add(new Field("order", "第二个添加的文档", fieldType));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new Field("title", "c", fieldType));
        doc.add(new Field("order", "第三个添加的文档", fieldType));
        indexWriter.updateDocument(new Term("title", "a"), doc);

        indexWriter.commit();

        DirectoryReader directoryReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(directoryReader);

        ScoreDoc[] result = searcher.search(new MatchAllDocsQuery(), 100).scoreDocs;
        for (ScoreDoc scoreDoc : result) {
            Document document = directoryReader.document(scoreDoc.doc);
            System.out.println("order: "+document.get("order")+"");
        }

        System.out.println("DONE");
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

    public static void main(String[] args) throws Exception{
        UpdateDocumentTest test = new UpdateDocumentTest();
        test.doSearch();
    }
}
