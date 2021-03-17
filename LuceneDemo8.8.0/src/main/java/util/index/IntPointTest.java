package util.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2021/3/9 7:12 下午
 */
public class IntPointTest {
    private static boolean test = true;
    private Directory directory;
    {
        try {
            if(test){
                FileOperation.deleteFile("./data");
            }
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
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        IndexReader reader1;
        if(!test){
            reader1 = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader1);
//            Sort sortByLevel = new Sort(new SortField("level", SortField.Type.STRING_VAL, false));
            Sort sortByLevel = new Sort(new SortField("level1", SortField.Type.STRING_VAL, false));
            ScoreDoc[] scoreDoc= searcher.search(new MatchAllDocsQuery(), 2, sortByLevel).scoreDocs;
            for (ScoreDoc doc : scoreDoc) {
                System.out.println(doc.doc);
            }
        }else {
            FieldType fieldType = new FieldType();
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            fieldType.setTokenized(true);
            fieldType.setStored(true);
            fieldType.setStoreTermVectorOffsets(true);
            fieldType.setStoreTermVectorPositions(true);
            fieldType.setStoreTermVectors(true);
            Document doc;
            int commitCount = 0;
            while (commitCount++ < 1){
                // 文档0
                doc = new Document();
                doc.add(new SortedDocValuesField("level", new BytesRef("a")));
                doc.add(new BinaryDocValuesField("level1", new BytesRef("df")));
                doc.add(new SortedDocValuesField("alevl", new BytesRef("adf")));
                doc.add(new Field("abc", "document0", fieldType));
                doc.add(new IntPoint("intPoint", 1,1));
                indexWriter.addDocument(doc);
                // 文档1
                doc = new Document();
                doc.add(new SortedDocValuesField("level", new BytesRef("d")));
                doc.add(new BinaryDocValuesField("level1", new BytesRef("da")));
                doc.add(new SortedDocValuesField("alevl", new BytesRef("df")));
                doc.add(new Field("abc", "document1", fieldType));
                doc.add(new IntPoint("intPoint", 2,5));
                indexWriter.addDocument(doc);
                // 文档2
                doc = new Document();
                doc.add(new Field("abc", "document2", fieldType));
                doc.add(new IntPoint("intPoint", 1,5));
                indexWriter.addDocument(doc);
                // 文档3
                doc = new Document();
                doc.add(new SortedDocValuesField("level", new BytesRef("c")));
                doc.add(new Field("abc", "document3", fieldType));
                doc.add(new IntPoint("intPoint", 2,5));
                indexWriter.addDocument(doc);
                // 文档4
                doc = new Document();
                doc.add(new SortedDocValuesField("level", new BytesRef("c")));
                doc.add(new Field("abc", "document3", fieldType));
                doc.add(new IntPoint("intPoint", 1,5));
                indexWriter.addDocument(doc);
                // 文档5
                doc = new Document();
                doc.add(new SortedDocValuesField("level", new BytesRef("c")));
                doc.add(new Field("abc", "document3", fieldType));
                doc.add(new IntPoint("intPoint", 2,5));
                indexWriter.addDocument(doc);
            }
//            indexWriter.updateBinaryDocValue(new Term("abc", new BytesRef("document1")), "level1", new BytesRef("aaaa"));
            indexWriter.commit();

//        IndexReader reader = DirectoryReader.open(indexWriter);
            IndexReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            Sort sortByLevel = new Sort(new SortField("level", SortField.Type.STRING_VAL, false));
            ScoreDoc[] scoreDoc= searcher.search(new MatchAllDocsQuery(), 2, sortByLevel).scoreDocs;
        }

    }

    public static void main(String[] args) throws Exception{
        IntPointTest test = new IntPointTest();
        test.doIndexAndSearch();
    }
}
