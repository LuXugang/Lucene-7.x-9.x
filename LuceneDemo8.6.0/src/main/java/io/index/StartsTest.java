package io.index;

import io.util.FileOperation;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Lu Xugang
 * @date 2020/11/2 4:00 PM
 */
public class StartsTest {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    IndexWriter indexWriter;

    public void doIndexAndSearch() throws Exception {
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Document doc ;
        int count = 0;
        while (count++ < 140000){
            doc = new Document();
            doc.add(new Field("content", RandomStringUtils.randomAlphabetic(5), type));
            indexWriter.addDocument(doc);
            if(count == 1000 | count == 3000 | count == 20000 | count == 100000){
                indexWriter.commit();
            }
        }
        indexWriter.commit();
        Document document;
        List<IndexableField> fields;
        IndexReader reader = DirectoryReader.open(directory);
        // 测试第一个段中的文档0（段内文档号）
        document = reader.document(30000);
        // 文档0有3个存储域
        fields = document.getFields();
        assert fields.size() == 3;
        assert fields.get(0).name().equals("content");
        assert fields.get(1).name().equals("content");
        assert fields.get(2).name().equals("author");
    }

    public static void main(String[] args) throws Exception{
        StartsTest test = new StartsTest();
        test.doIndexAndSearch();
    }
}
