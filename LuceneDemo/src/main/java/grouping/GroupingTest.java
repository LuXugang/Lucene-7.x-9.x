package grouping;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.grouping.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * @author Lu Xugang
 * @date 2019-01-07 17:20
 */
public class GroupingTest {
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
    private IndexWriter writer;

    private void doSearch() throws Exception{
        String groupField = "author";
        FieldType customType = new FieldType();
        customType.setStored(true);
        writer = new IndexWriter(directory, conf);

        Document doc = new Document();
        // sortedDocValues
        addGroupField(doc, groupField, "author1");
        doc.add(new TextField("content", "random text", Field.Store.YES));
        doc.add(new Field("id", "1", customType));
        writer.addDocument(doc);

        // 1
        doc = new Document();
        addGroupField(doc, groupField, "author1");
        doc.add(new TextField("content", "some more random text", Field.Store.YES));
        doc.add(new Field("id", "2", customType));
        writer.addDocument(doc);

        // 2
        doc = new Document();
        addGroupField(doc, groupField, "author1");
        doc.add(new TextField("content", "some more random textual data", Field.Store.YES));
        doc.add(new Field("id", "3", customType));
        writer.addDocument(doc);

        // 3
        doc = new Document();
        addGroupField(doc, groupField, "author2");
        doc.add(new TextField("content", "some random text", Field.Store.YES));
        doc.add(new Field("id", "4", customType));
        writer.addDocument(doc);

        // 4
        doc = new Document();
        addGroupField(doc, groupField, "author3");
        doc.add(new TextField("content", "some more random text", Field.Store.YES));
        doc.add(new Field("id", "5", customType));
        writer.addDocument(doc);

        // 5
        doc = new Document();
        addGroupField(doc, groupField, "author3");
        doc.add(new TextField("content", "random", Field.Store.YES));
        doc.add(new Field("id", "6", customType));
        writer.addDocument(doc);

        // 6 -- no author field
        doc = new Document();
        doc.add(new TextField("content", "random word stuck in alot of other text", Field.Store.YES));
        doc.add(new Field("id", "6", customType));
        writer.addDocument(doc);

        IndexReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);

        // 根据打分进行排序
        final Sort groupSort = Sort.RELEVANCE;

        final FirstPassGroupingCollector<?> c1 = createRandomFirstPassCollector(groupField, groupSort, 2);
        searcher.search(new TermQuery(new Term("content", "random")), c1);

        final TopGroupsCollector<?> c2 = createSecondPassCollector(c1, groupSort, Sort.RELEVANCE, 0, 5, true, true, true);
        searcher.search(new TermQuery(new Term("content", "random")), c2);

        final TopGroups<?> groups = c2.getTopGroups(0);

        System.out.println(groups.totalHitCount);
        reader.close();
        directory.close();
    }

    private void addGroupField(Document doc, String groupField, String value) {
        doc.add(new SortedDocValuesField(groupField, new BytesRef(value)));
    }

    private FirstPassGroupingCollector<?> createRandomFirstPassCollector(String groupField, Sort groupSort, int topDocs) throws IOException {
//        ValueSource vs = new BytesRefFieldSource(groupField);
//        return new FirstPassGroupingCollector<>(new ValueSourceGroupSelector(vs, new HashMap<>()), groupSort, topDocs);
            return new FirstPassGroupingCollector<>(new TermGroupSelector(groupField), groupSort, topDocs);
    }

    private <T> TopGroupsCollector<T> createSecondPassCollector(FirstPassGroupingCollector firstPassGroupingCollector,
                                                                Sort groupSort,
                                                                Sort sortWithinGroup,
                                                                int groupOffset,
                                                                int maxDocsPerGroup,
                                                                boolean getScores,
                                                                boolean getMaxScores,
                                                                boolean fillSortFields) throws IOException {

        Collection<SearchGroup<T>> searchGroups = firstPassGroupingCollector.getTopGroups(groupOffset, fillSortFields);
        return new TopGroupsCollector<>(firstPassGroupingCollector.getGroupSelector(), searchGroups, groupSort, sortWithinGroup, maxDocsPerGroup, getScores, getMaxScores, fillSortFields);
    }

    public static void main(String[] args) throws Exception{
        GroupingTest groupingTest = new GroupingTest();
        groupingTest.doSearch();

    }


}
