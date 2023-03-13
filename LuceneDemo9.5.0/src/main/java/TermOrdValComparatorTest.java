import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import util.FileOperation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TermOrdValComparatorTest {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Analyzer analyzer = new WhitespaceAnalyzer();
    private final IndexWriterConfig conf = new IndexWriterConfig(analyzer);

    public void doSearch() throws Exception {
        SortField field = new SortedSetSortField("name", false);
        field.setMissingValue(SortField.STRING_LAST);
        Sort indexSearch = new Sort(field);
        conf.setUseCompoundFile(false);
//        conf.setIndexSort(indexSearch);
        IndexWriter indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;
        // 文档0

        int count = 0;
        while (count++ < 200000){
            doc = new Document();
            if(count == 999 || count ==  1000|| count == 1001){
                String aaa = "d" + new Random().nextInt();
                doc.add(new StringField("name", aaa, Field.Store.YES));
                doc.add(new SortedSetDocValuesField("name", new BytesRef("z")));
                indexWriter.addDocument(doc);
            }if(count == 1002){
                doc.add(new StringField("name", "y", Field.Store.YES));
                doc.add(new SortedSetDocValuesField("name", new BytesRef("y")));
                indexWriter.addDocument(doc);
            }else {
                String aaa = "d" + new Random().nextInt();
                doc.add(new StringField("name", aaa, Field.Store.YES));
                doc.add(new SortedSetDocValuesField("name", new BytesRef(aaa)));
                indexWriter.addDocument(doc);
            }

        }

        indexWriter.commit();
        indexWriter.forceMerge(1);
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        System.out.println("document count : "+reader.maxDoc()+"");


        Query query = new MatchAllDocsQuery();
        SortField searchSortField = new SortedSetSortField("name", false);
        searchSortField.setMissingValue(SortField.STRING_LAST);
        Sort searchSort = new Sort(searchSortField);
        // 返回Top5的结果
        int resultTopN = 1000;

        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN, searchSort).scoreDocs;
//        ScoreDoc[] scoreDocs = searcher.search(query, resultTopN).scoreDocs;

        System.out.println("Total Result Number: "+scoreDocs.length+"");
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            // 输出满足查询条件的 文档号
            System.out.println(""+i+"：doc id: "+scoreDoc.doc+": 文档"+reader.document(scoreDoc.doc).get("name")+"");
        }
    }

    public static void main(String[] args) throws Exception{
        TermOrdValComparatorTest test = new TermOrdValComparatorTest();
        test.doSearch();
    }
}
