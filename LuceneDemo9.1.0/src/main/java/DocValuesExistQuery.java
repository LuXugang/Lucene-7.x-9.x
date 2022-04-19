import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnVectorFieldExistsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.VectorUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class DocValuesExistQuery {
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
        SortField sortField = new SortedNumericSortField("number2", SortField.Type.LONG);
        sortField.setMissingValue(1L);
        Sort indexSort = new Sort(sortField);
//        Sort indexSort = new Sort(new SortedNumericSortField("number", SortField.Type.LONG, true, SortedNumericSelector.Type.MAX));
        conf.setUseCompoundFile(false);
        conf.setMergeScheduler(new SerialMergeScheduler());
        conf.setIndexSort(indexSort);
        indexWriter = new IndexWriter(directory, conf);
        Random random = new Random();
        Document doc;

        int count = 0 ;
        while (count++ < 10000){
            if(count == 2000){
                doc = new Document();
                doc.add(new StringField("field", "a", Field.Store.NO));
                indexWriter.addDocument(doc);
            }else {
                doc = new Document();
                doc.add(new KnnVectorField("vector", randomVector(5, random)));
                indexWriter.addDocument(doc);
            }
        }
        doc = new Document();
        doc.add(new StringField("field", "a", Field.Store.NO));
        doc.add(new KnnVectorField("vector", randomVector(5, random)));
        indexWriter.addDocument(doc);

        doc = new Document();
        doc.add(new StringField("field", "a", Field.Store.NO));
        doc.add(new KnnVectorField("vector", randomVector(5, random)));
        indexWriter.addDocument(doc);


        indexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery booleanQuery =
                new BooleanQuery.Builder()
                        .add(new TermQuery(new Term("field", "a")), BooleanClause.Occur.MUST)
                        .add(new KnnVectorFieldExistsQuery("vector"), BooleanClause.Occur.MUST)
                        .build();

        // 返回Top5的结果
        int resultTopN = 10000000;

        int hits = searcher.count(booleanQuery);
        System.out.println("hits: "+hits+"");



        System.out.println("DONE");
    }

    private float[] randomVector(int dim, Random random) {
        float[] v = new float[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = random.nextFloat();
        }
        VectorUtil.l2normalize(v);
        return v;
    }
    public static void main(String[] args) throws Exception{
        DocValuesExistQuery test = new DocValuesExistQuery();
        test.doSearch();
    }
}
