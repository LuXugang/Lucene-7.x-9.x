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
import java.util.Random;

/**
 * 文章中的demo 不要修改
 */
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

    public void doSearch() throws Exception {
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        Document doc;

        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        SortedSetSortField indexSortField1 = new SortedSetSortField("sort0", true, SortedSetSelector.Type.MIN);
        SortedSetSortField indexSortField2 = new SortedSetSortField("sort1", true, SortedSetSelector.Type.MAX);
        SortField[] indexSortFields = new SortField[2];
        indexSortFields[0] = indexSortField1;
        indexSortFields[1] = indexSortField2;
        Sort indexSort = new Sort(indexSortFields);
        conf.setIndexSort(indexSort);
        IndexWriter indexWriter = new IndexWriter(directory, conf);

        // 文档0
        doc = new Document();
        doc.add(new Field("sequence", "文档0", fieldType));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("c1")));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("c2")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("a1")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("a2")));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new Field("sequence", "文档1", fieldType));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("b1")));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("c1")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("e1")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("e2")));
        indexWriter.addDocument(doc);
        // 文档2
        doc = new Document();
        doc.add(new Field("sequence", "文档2", fieldType));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("b1")));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("b2")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("e2")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("f2")));
        indexWriter.addDocument(doc);
        // 文档3
        doc = new Document();
        doc.add(new Field("sequence", "文档3", fieldType));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("b1")));
        doc.add(new SortedSetDocValuesField("sort0", new BytesRef("b2")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("e1")));
        doc.add(new SortedSetDocValuesField("sort1", new BytesRef("e2")));
        indexWriter.addDocument(doc);
        // 文档4
        doc = new Document();
        doc.add(new Field("sequence", "文档4", fieldType));
        indexWriter.addDocument(doc);
        indexWriter.commit();
        DirectoryReader reader= DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        boolean resultToSort = new Random().nextBoolean();
        System.out.println(resultToSort ? "sorted in search phase" : "not sorted in search phase");
        SortedSetSortField searchSortField1 = new SortedSetSortField("sort0", true, SortedSetSelector.Type.MAX);
        SortedSetSortField searchSortField2 = new SortedSetSortField("sort1", true, SortedSetSelector.Type.MIN);
        SortField[] searchSortFields = new SortField[2];
        searchSortFields[0] = searchSortField1;
        searchSortFields[1] = searchSortField2;
        Sort searchSort = new Sort(searchSortFields);
        ScoreDoc[] result = resultToSort
                ?
                searcher.search(new MatchAllDocsQuery(), 100, searchSort).scoreDocs
                :
                searcher.search(new MatchAllDocsQuery(), 100).scoreDocs;
        for (ScoreDoc scoreDoc : result) {
            System.out.println("段内排序后的文档号: "+scoreDoc.doc+" VS 段内排序前的文档: "+reader.document(scoreDoc.doc).get("sequence")+"");
        }

        System.out.println("DONE");
    }
    public static void main(String[] args) throws Exception{
        IndexSortTest test = new IndexSortTest();
        test.doSearch();
    }
}
