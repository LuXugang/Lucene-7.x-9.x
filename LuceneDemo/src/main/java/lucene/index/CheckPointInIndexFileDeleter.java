package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/12/3 5:28 下午
 */
public class CheckPointInIndexFileDeleter {
    private Directory directory ;
    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig oldConf = new IndexWriterConfig(analyzer);
    private IndexWriter oldIndexWriter;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = FSDirectory.open(Paths.get("./data"));
            oldConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            oldConf.setIndexDeletionPolicy(NoDeletionPolicy.INSTANCE);
            oldConf.setMergePolicy(NoMergePolicy.INSTANCE);
            oldConf.setCommitOnClose(false);
            oldIndexWriter = new IndexWriter(directory, oldConf);
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

        Document doc;
        // 文档0
        doc = new Document();
        doc.add(new StringField("author", "Lily", Field.Store.YES));
        doc.add(new StringField("title", "care", Field.Store.YES));
        doc.add(new NumericDocValuesField("age", -2));
        oldIndexWriter.addDocument(doc);
        oldIndexWriter.commit();
        // 文档1
        doc = new Document();
        doc.add(new StringField("author", "Luxugang", Field.Store.YES));
        doc.add(new StringField("title", "whatEver", Field.Store.YES));
        doc.add(new NumericDocValuesField("age", 0));
        oldIndexWriter.addDocument(doc);
        // 执行commit()操作后，生成索引文件segments_1
        oldIndexWriter.commit();
        // 以NRT方式获得一个reader, 见文章 https://www.amazingkoala.com.cn/Lucene/Index/2019/0920/95.html
        DirectoryReader reader = DirectoryReader.open(oldIndexWriter);
        // 文档2
        doc = new Document();
        doc.add(new StringField("author", "Jay", Field.Store.YES));
        doc.add(new StringField("title", "careFi", Field.Store.YES));
        doc.add(new NumericDocValuesField("age", 0));
        oldIndexWriter.addDocument(doc);
        // 通过NRT获得最新的索引信息，并且没有执行commit()提交
        reader = DirectoryReader.openIfChanged(reader, oldIndexWriter);
        IndexCommit indexCommit = reader.getIndexCommit();
        IndexWriterConfig newConf = new IndexWriterConfig(analyzer);
        newConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        newConf.setIndexCommit(indexCommit);
        // 关闭当前IndexWriter，否则新的IndexWriter无法作用同一个索引目录
        oldIndexWriter.close();
        // 使用相同的索引目录directory
        IndexWriter newIndexWriter = new IndexWriter(directory, newConf);
        // 在第70行代码，尽在添加了一篇文档后，尽管没有执行commit()操作，但是通过NRT获得最新的索引信息后,newIndexWriter仍然有这篇文档的索引数据
        DirectoryReader newReader = DirectoryReader.open(newIndexWriter);
        String authorValue = newReader.document(2).get("author");
        if(authorValue.equals("Jay")){
            System.out.println("OK");
        }


    }
    public static void main(String[] args) throws Exception {
        CheckPointInIndexFileDeleter deleter = new CheckPointInIndexFileDeleter();
        deleter.doIndex();
    }
}


