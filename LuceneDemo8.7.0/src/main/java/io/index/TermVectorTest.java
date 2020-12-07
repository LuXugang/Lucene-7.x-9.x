package io.index;

import io.util.FileOperation;
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
 * @date 2020/11/16 17:41
 */
public class TermVectorTest {
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
        String sortedField = "sortByNumber";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorOffsets(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorPositions(true);
        Document doc ;
        int count = 0;
        while (count++ < 100){
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "the book is book", type));
            doc.add(new Field("title", "book", type));
            doc.add(new StringField("author", "book", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档1
            type.setStoreTermVectorPayloads(false);
            doc = new Document();
            doc.add(new Field("content", "the fake news is news", type));
            doc.add(new Field("title", "news", type));
            doc.add(new StringField("author", "book", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", type));
            doc.add(new Field("title", "news", type));
            doc.add(new StringField("author", "book", Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
    }

    public static void main(String[] args) throws Exception{
        TermVectorTest test = new TermVectorTest();
        test.doIndexAndSearch();
    }
}
