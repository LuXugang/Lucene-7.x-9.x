package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new NIOFSDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(true);
        indexWriter = new IndexWriter(directory, conf);
        int i = 100000000;
        Random random = new Random();
        for (int i1 = 0; i1 < i; i1++) {
           Document document = new Document();

            document.add(new LongPoint("long", i));
            document.add(new SortedNumericDocValuesField("long", i));

            int r = random.nextInt(1000000);
            document.add(new LongPoint("long300", r));
            document.add(new SortedNumericDocValuesField("long300", r));

            r = random.nextInt(100000);
            document.add(new LongPoint("long3000", r));
            document.add(new SortedNumericDocValuesField("long3000", r));

            r = random.nextInt(10000);
            document.add(new LongPoint("long30000", r));
            document.add(new SortedNumericDocValuesField("long30000", r));

            r = random.nextInt(1000);
            document.add(new LongPoint("long300000", r));
            document.add(new SortedNumericDocValuesField("long300000", r));

            r = random.nextInt(100);
            document.add(new LongPoint("long3000000", r));
            document.add(new SortedNumericDocValuesField("long3000000", r));

            indexWriter.addDocument(document);
            if(i1 % 1000000 == 0){
                System.out.println("doc numbers: "+i1+"");
            }
        }
        indexWriter.close();
        directory.close();


    }
    public static void main(String[] args) throws Exception{
        Test test = new Test();
        test.doSearch();
    }
}
