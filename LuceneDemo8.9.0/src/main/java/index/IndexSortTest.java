package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;

public class IndexSortTest {
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
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        SortedSetSortField sortField = new SortedSetSortField("sort1", true);
        Sort sort = new Sort(sortField);
        conf.setIndexSort(sort);
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        indexWriter = new IndexWriter(directory, conf);
        Document doc;

        // 文档0
        doc = new Document();
        doc.add(new Field("title", "i love china china", fieldType));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("c")));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new Field("body", "china", fieldType));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("a")));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("e")));
        doc.add(new Field("title", "i love china china", fieldType));
        doc.add(new Field("body", "china", fieldType));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new Field("body", "china", fieldType));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("b")));
        indexWriter.addDocument(doc);

        indexWriter.commit();

        Query query = new TermQuery(new Term("title", new BytesRef("china")));

        DirectoryReader directoryReader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(directoryReader);

        ScoreDoc[] result = searcher.search(query, 100).scoreDocs;
//        ScoreDoc[] result = searcher.search(booleanQuery, 100).scoreDocs;
        for (ScoreDoc scoreDoc : result) {
            System.out.println("文档号: "+scoreDoc.doc+" 文档分数: "+scoreDoc.score+"");
        }

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        IndexSortTest test = new IndexSortTest();
        test.doSearch();
    }
}
