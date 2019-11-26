package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/11/26 3:03 下午
 */
public class ValidateIndexSort {
    private Directory directory;
    private Directory directory1;

    {
        try {
            FileOperation.deleteFile("./data");
            FileOperation.deleteFile("./data2");
            // newIndexWriter的工作目录
            directory = new MMapDirectory(Paths.get("./data"));
            // oldIndexWriter的工作目录
            directory1 = new MMapDirectory(Paths.get("./data2"));
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
        IndexWriterConfig oldConf = new IndexWriterConfig(analyzer);
        IndexWriter oldIndexWriter = new IndexWriter(directory1, oldConf);

        if(oldConf.getIndexSort() == null){
            System.out.println("oldIndexWriter has no IndexSort");
        }

        int count = 0;
        Document doc = new Document();
        // oldIndexWriter添加三篇文档
        while (count++ < 1) {
            // 文档0
            doc.add(new Field("author", "aab b aab aabbcc ", type));
            doc.add(new Field("content", "a b", type));
            doc.add(new IntPoint("intPoitn", 3, 4, 6));
            oldIndexWriter.addDocument(doc);

            // 文档1
            doc = new Document();
            doc.add(new TextField("author", "a", Field.Store.YES));
            doc.add(new TextField("content", "a b c h", Field.Store.YES));
            doc.add(new TextField("title", "d a", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", -1));
            doc.add(new IntPoint("intPoitn", 3, 5, 6));
            oldIndexWriter.addDocument(doc);

            // 文档2
            doc = new Document();
            doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
            doc.add(new TextField("content", "a c b e", Field.Store.YES));
            doc.add(new NumericDocValuesField("sortByNumber", 4));
            oldIndexWriter.addDocument(doc);
            oldIndexWriter.flush();
        }
        oldIndexWriter.commit();
        oldIndexWriter.close();

        IndexWriterConfig newConf = new IndexWriterConfig(analyzer);
        newConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        SortField indexSortField = new SortField("sortByNumber", SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
        newConf.setIndexSort(indexSort);
        IndexWriter newIndexWriter = new IndexWriter(directory, newConf);
        newIndexWriter.addIndexes(directory1);
        newIndexWriter.commit();

        DirectoryReader reader = DirectoryReader.open(newIndexWriter);
        System.out.println("Doc number: "+reader.maxDoc()+"");

    }

    public static void main(String[] args) throws Throwable{
        ValidateIndexSort test = new ValidateIndexSort();
        test.doIndex();
    }
}
