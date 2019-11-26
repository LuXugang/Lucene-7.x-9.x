package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/11/26 2:44 下午
 */
public class OldVersionTest {
    private Directory directory;
    private Directory directory1;

    {
        try {
//            FileOperation.deleteFile("./data");
            // newIndexWriter的工作目录
            // oldIndexWriter的工作目录
            directory = new MMapDirectory(Paths.get("./data"));
            directory1 = new MMapDirectory(Paths.get("./Lucene6_5_0"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doIndex() throws Exception {
        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Analyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        SortField indexSortField = new SortField("sortByNumber", SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
        conf.setIndexSort(indexSort);
        IndexWriter indexWriter = new IndexWriter(directory, conf);


        int count = 0;
        Document doc = new Document();
        // oldIndexWriter添加三篇文档
        while (count++ < 1) {
            // 文档0
            doc.add(new Field("author", "aab b aab aabbcc ", type));
            doc.add(new Field("content", "a b", type));
            doc.add(new IntPoint("intPoitn", 3, 4, 6));
            indexWriter.addDocument(doc);

            // 文档1
            doc = new Document();
            doc.add(new TextField("author", "a", Field.Store.YES));
            doc.add(new TextField("content", "a b c h", Field.Store.YES));
            doc.add(new TextField("title", "d a", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", -1));
            doc.add(new IntPoint("intPoitn", 3, 5, 6));
            indexWriter.addDocument(doc);

            // 文档2
            doc = new Document();
            doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
            doc.add(new TextField("content", "a c b e", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", 4));
            indexWriter.addDocument(doc);
            indexWriter.flush();
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(directory1);

        System.out.println("abc");
//        indexWriter.addIndexes(directory1);

    }

    public static void main(String[] args) throws Throwable{
        OldVersionTest test = new OldVersionTest();
        test.doIndex();
    }
}
