import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    IndexWriter indexWriter;

    public void doIndexAndSearch() throws Exception {
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        String sortedField = "sortByNumber";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
        Sort indexSort = new Sort(indexSortField);;
        conf.setIndexSort(indexSort);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS);
        Document doc ;
        int count = 0;
        while (count++ < 1){
            // 文档0
            doc = new Document();
            doc.add(new Field("content", "abc", type));
            doc.add(new Field("content", "cd", type));
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
            doc.add(new NumericDocValuesField(sortedField, 5));
            doc.add(new StoredField("author", 3));
            indexWriter.addDocument(doc)    ;
            // 文档1
            doc = new Document();
            doc.add(new StringField("attachment", "cd", Field.Store.NO));
            doc.add(new NumericDocValuesField(sortedField, 2));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new Field("content", "the name is name", type));
            doc.add(new NumericDocValuesField(sortedField, 7));
            indexWriter.addDocument(doc);
            indexWriter.flush();
        }
        indexWriter.commit();
        Document document;
        List<IndexableField> fields;

        IndexReader reader = DirectoryReader.open(directory);
        for (LeafReaderContext leaf : reader.leaves()) {
           Terms terms = leaf.reader().terms("content");
            System.out.println("abc");
        }
        reader.document(0);

        IndexSearcher searcher = new IndexSearcher(reader);
        List<BytesRef> list = new ArrayList<>();
        list.add(new BytesRef("abc"));
        list.add(new BytesRef("abc"));
        Query query = new TermInSetQuery("content", list);
        searcher.search(query, 1000);
    }

    public static void main(String[] args) throws Exception{
        IndexSortTest test = new IndexSortTest();
        test.doIndexAndSearch();
    }
}
