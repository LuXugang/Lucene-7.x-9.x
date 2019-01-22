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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lu Xugang
 * @date 2019-01-18 16:28
 */
public class GroupingSearchTest {
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

    public void doSearch() throws Exception{
        String groupField = "author";
        FieldType customType = new FieldType();
        customType.setStored(true);
        writer = new IndexWriter(directory, conf);
        List<Document> documents = new ArrayList<>();
        boolean canUseIDV = true;
        // 0
        Document doc = new Document();
        addGroupField(doc, groupField, "author1", canUseIDV);
        doc.add(new TextField("content", "random text", Field.Store.YES));
        doc.add(new Field("id", "1", customType));
        documents.add(doc);

        // 1
        doc = new Document();
        addGroupField(doc, groupField, "author1", canUseIDV);
        doc.add(new TextField("content", "some more random text", Field.Store.YES));
        doc.add(new Field("id", "2", customType));
        documents.add(doc);

        // 2
        doc = new Document();
        addGroupField(doc, groupField, "author1", canUseIDV);
        doc.add(new TextField("content", "some more random textual data", Field.Store.YES));
        doc.add(new Field("id", "3", customType));
        doc.add(new StringField("groupend", "x", Field.Store.NO));
        documents.add(doc);
        writer.addDocuments(documents);
        documents.clear();

        // 3
        doc = new Document();
        addGroupField(doc, groupField, "author2", canUseIDV);
        doc.add(new TextField("content", "some random text", Field.Store.YES));
        doc.add(new Field("id", "4", customType));
        doc.add(new StringField("groupend", "x", Field.Store.NO));
        writer.addDocument(doc);

        // 4
        doc = new Document();
        addGroupField(doc, groupField, "author3", canUseIDV);
        doc.add(new TextField("content", "some more random text", Field.Store.YES));
        doc.add(new Field("id", "5", customType));
        documents.add(doc);

        // 5
        doc = new Document();
        addGroupField(doc, groupField, "author3", canUseIDV);
        doc.add(new TextField("content", "random", Field.Store.YES));
        doc.add(new Field("id", "6", customType));
        doc.add(new StringField("groupend", "x", Field.Store.NO));
        documents.add(doc);
        writer.addDocuments(documents);
        documents.clear();

        // 6 -- no author field
        doc = new Document();
        doc.add(new TextField("content", "random word stuck in alot of other text", Field.Store.YES));
        doc.add(new Field("id", "6", customType));
        doc.add(new StringField("groupend", "x", Field.Store.NO));

        writer.addDocument(doc);

        IndexReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);

        Sort groupSort = Sort.RELEVANCE;
        GroupingSearch groupingSearch = createRandomGroupingSearch(groupField, groupSort, 5, canUseIDV);
        groupingSearch.setAllGroupHeads(true);
        groupingSearch.setAllGroups(true);

        TopGroups<?> groups = groupingSearch.search(searcher, new TermQuery(new Term("content", "random")), 0, 2);

        System.out.println(groups.totalHitCount);
        reader.close();
        directory.close();
    }

    private void addGroupField(Document doc, String groupField, String value, boolean canUseIDV) {
        doc.add(new TextField(groupField, value, Field.Store.YES));
        if (canUseIDV) {
            doc.add(new SortedDocValuesField(groupField, new BytesRef(value)));
        }
    }

    private GroupingSearch createRandomGroupingSearch(String groupField, Sort groupSort, int docsInGroup, boolean canUseIDV) {
        GroupingSearch groupingSearch;
        groupingSearch = new GroupingSearch(groupField);
        groupingSearch.setGroupSort(groupSort);
        groupingSearch.setGroupDocsLimit(docsInGroup);
        return groupingSearch;
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
        GroupingSearchTest grouping = new GroupingSearchTest();
        grouping.doSearch();

    }


}
