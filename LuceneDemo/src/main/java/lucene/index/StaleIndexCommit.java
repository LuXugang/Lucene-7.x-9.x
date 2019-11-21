package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/11/19 8:17 下午
 */
public class StaleIndexCommit {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
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
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        int count = 0;
        Document doc = new Document();
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
        // 执行commit()操作后，生成segments_1文件
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        // 获得indexCommit
        IndexCommit indexCommit = reader.getIndexCommit();

        // IndexWriter再次添加文档，并且执行commit()操作
        doc = new Document();
        doc.add(new TextField("author", "aab aab aabb ", Field.Store.YES));
        doc.add(new TextField("content", "a c b e", Field.Store.YES));
        doc.add(new NumericDocValuesField("sortByNumber", 4));
        indexWriter.addDocument(doc);
        // 执行commit()操作后，生成segments_2文件, 由于使用了默认的KeepOnlyLastCommitDeletionPolicy,故segments_1文件被删除
        indexWriter.commit();

        // indexWriter关闭，释放索引文件锁，使得可以让新的IndexWriter对象操作同一个索引目录
        indexWriter.close();

        // 生成一个新的IndexWriter对象，并且设置IndexCommit配置
        IndexWriterConfig newConf = new IndexWriterConfig(analyzer);
        newConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        // IndexCommit中保留的是segment_1文件对应的索引信息，但此时索引目录没有该文件了
        newConf.setIndexCommit(indexCommit);

        // 使用相同的索引目录directory
        IndexWriter newIndexWriter = new IndexWriter(directory, newConf);
        System.out.println("抛出异常");

    }
    public static void main(String[] args) throws Exception{
        StaleIndexCommit staleIndexCommit= new StaleIndexCommit();
        staleIndexCommit.doIndex();
    }

}
