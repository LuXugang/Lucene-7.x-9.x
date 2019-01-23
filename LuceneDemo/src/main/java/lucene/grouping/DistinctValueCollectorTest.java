package lucene.grouping;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.function.valuesource.BytesRefFieldSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.grouping.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Lu Xugang
 * @date 2019-01-21 16:46
 */
public class DistinctValueCollectorTest {
        private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final String GROUP_FIELD = "author";
    private static final String COUNT_FIELD = "publisher";

    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter w;

    public void doSearch() throws Exception{
        w = new IndexWriter(directory, conf);
        Document doc = new Document();
        addField(doc, GROUP_FIELD, "1");
        addField(doc, COUNT_FIELD, "1");
        doc.add(new TextField("content", "random text", Field.Store.NO));
        doc.add(new StringField("id", "1", Field.Store.NO));
        w.addDocument(doc);

        // 1
        doc = new Document();
        addField(doc, GROUP_FIELD, "1");
        addField(doc, COUNT_FIELD, "1");
        doc.add(new TextField("content", "some more random text blob", Field.Store.NO));
        doc.add(new StringField("id", "2", Field.Store.NO));
        w.addDocument(doc);

        // 2
        doc = new Document();
        addField(doc, GROUP_FIELD, "1");
        addField(doc, COUNT_FIELD, "2");
        doc.add(new TextField("content", "some more random textual data", Field.Store.NO));
        doc.add(new StringField("id", "3", Field.Store.NO));
        w.addDocument(doc);
        w.commit(); // To ensure a second segment

        // 3 -- no count field
        doc = new Document();
        addField(doc, GROUP_FIELD, "2");
        doc.add(new TextField("content", "some random text", Field.Store.NO));
        doc.add(new StringField("id", "4", Field.Store.NO));
        w.addDocument(doc);

        // 4
        doc = new Document();
        addField(doc, GROUP_FIELD, "3");
        addField(doc, COUNT_FIELD, "1");
        doc.add(new TextField("content", "some more random text", Field.Store.NO));
        doc.add(new StringField("id", "5", Field.Store.NO));
        w.addDocument(doc);

        // 5
        doc = new Document();
        addField(doc, GROUP_FIELD, "3");
        addField(doc, COUNT_FIELD, "1");
        doc.add(new TextField("content", "random blob", Field.Store.NO));
        doc.add(new StringField("id", "6", Field.Store.NO));
        w.addDocument(doc);

        // 6 -- no author field
        doc = new Document();
        doc.add(new TextField("content", "random word stuck in alot of other text", Field.Store.YES));
        addField(doc, COUNT_FIELD, "1");
        doc.add(new StringField("id", "6", Field.Store.NO));
        w.addDocument(doc);

        Comparator<DistinctValuesCollector.GroupCount<Comparable<Object>, Comparable<Object>>> cmp = (groupCount1, groupCount2) -> {
            if (groupCount1.groupValue == null) {
                if (groupCount2.groupValue == null) {
                    return 0;
                }
                return -1;
            } else if (groupCount2.groupValue == null) {
                return 1;
            } else {
                return groupCount1.groupValue.compareTo(groupCount2.groupValue);
            }
        };
        IndexReader reader = DirectoryReader.open(w);
        IndexSearcher searcher = new IndexSearcher(reader);

        FirstPassGroupingCollector<Comparable<Object>> firstCollector = createRandomFirstPassCollector(new Sort(), GROUP_FIELD, 4);
        searcher.search(new TermQuery(new Term("content", "random")), firstCollector);
        DistinctValuesCollector<Comparable<Object>, Comparable<Object>> distinctValuesCollector
                = createDistinctCountCollector(firstCollector, COUNT_FIELD);
        searcher.search(new TermQuery(new Term("content", "random")), distinctValuesCollector);

        List<DistinctValuesCollector.GroupCount<Comparable<Object>, Comparable<Object>>> gcs = distinctValuesCollector.getGroups();
        gcs.sort(cmp);
        System.out.println("hah");

    }



    private <T> FirstPassGroupingCollector<T> createRandomFirstPassCollector(Sort groupSort, String groupField, int topNGroups) throws IOException {
            return (FirstPassGroupingCollector<T>) new FirstPassGroupingCollector<>(new TermGroupSelector(groupField), groupSort, topNGroups);
    }

    private <T extends Comparable<Object>, R extends Comparable<Object>> DistinctValuesCollector<T, R> createDistinctCountCollector(FirstPassGroupingCollector<T> firstPassGroupingCollector,
                                                                                                                                    String countField) throws IOException {
        Collection<SearchGroup<T>> searchGroups = firstPassGroupingCollector.getTopGroups(0, false);
        GroupSelector<T> selector = firstPassGroupingCollector.getGroupSelector();
        if (ValueSourceGroupSelector.class.isAssignableFrom(selector.getClass())) {
            GroupSelector gs = new ValueSourceGroupSelector(new BytesRefFieldSource(countField), new HashMap<>());
            return new DistinctValuesCollector<>(selector, searchGroups, gs);
        } else {
            GroupSelector ts = new TermGroupSelector(countField);
            return new DistinctValuesCollector<>(selector, searchGroups, ts);
        }
    }
    private void addField(Document doc, String field, String value) {
        doc.add(new SortedDocValuesField(field, new BytesRef(value)));
    }


    public static void main(String[] args) throws Exception{
            DistinctValueCollectorTest test = new DistinctValueCollectorTest();
            test.doSearch();
    }


}
