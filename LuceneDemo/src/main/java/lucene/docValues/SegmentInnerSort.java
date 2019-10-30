package lucene.docValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019/10/30 3:06 下午
 */
public class SegmentInnerSort {

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
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        SortField sortField1 = new SortField("age", SortField.Type.LONG);
        SortField sortField2 = new SortField("label", SortField.Type.STRING);
        SortField[] allSortFields = new SortField[] { sortField1, sortField2};
        Sort sort = new Sort(allSortFields);
        indexWriterConfig.setIndexSort(sort);

        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        // 文档0
        Document doc = new Document();
        doc.add(new Field("author", "author0", type));
        doc.add(new NumericDocValuesField("age", 10));
        doc.add(new SortedDocValuesField("label", new BytesRef("f")));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new Field("author", "author1", type));
        doc.add(new NumericDocValuesField("age", 20));
        doc.add(new SortedDocValuesField("label", new BytesRef("c")));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new Field("author", "author2", type));
        doc.add(new NumericDocValuesField("age", 20));
        doc.add(new SortedDocValuesField("label", new BytesRef("b")));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new Field("author", "author3", type));
        doc.add(new NumericDocValuesField("age", 60));
        doc.add(new SortedDocValuesField("label", new BytesRef("a")));
        indexWriter.addDocument(doc);
        // 文档4
        doc = new Document();
        doc.add(new Field("author", "author4", type));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        // 文档5
        doc = new Document();
        doc.add(new Field("author", "author5", type));
        doc.add(new NumericDocValuesField("age", 60));
        doc.add(new SortedDocValuesField("label", new BytesRef("a")));
        indexWriter.addDocument(doc);

        DirectoryReader reader = DirectoryReader.open(indexWriter);
        int docId = 0;
        while (docId < reader.maxDoc()){
            System.out.println("docId: "+docId+", 域名: author, 域值: "+reader.document(docId).get("author")+"");
            docId++;
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = new MatchAllDocsQuery();
        ScoreDoc[] scoreDoc = searcher.search(query, 100).scoreDocs;
        System.out.println("hha");
    }

    public static void main(String[] args) throws Throwable{
        SegmentInnerSort sort = new SegmentInnerSort();
        sort.doSearch();
    }
}
