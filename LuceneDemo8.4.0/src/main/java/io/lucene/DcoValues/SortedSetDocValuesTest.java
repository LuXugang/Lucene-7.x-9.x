package io.lucene.DcoValues;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/5/28 4:21 下午
 */
public class SortedSetDocValuesTest {
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

    public void doIndexAndSearch() throws Exception {
        conf.setUseCompoundFile(false);
        conf.setMergePolicy(NoMergePolicy.INSTANCE);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        int count = 0;
        Document doc;
        while (count++ < 1) {
            // 文档0
            doc = new Document();
            doc.add(new SortedSetDocValuesField("level", new BytesRef("a")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("h")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("l")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("r")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档1
            doc = new Document();
            doc.add(new SortedSetDocValuesField("level", new BytesRef("b")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("g")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("i")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("q")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档2
            doc = new Document();
            doc.add(new SortedSetDocValuesField("level", new BytesRef("c")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("f")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("j")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("o")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
            // 文档3
            doc = new Document();
            doc.add(new SortedSetDocValuesField("level", new BytesRef("d")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("e")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("k")));
            doc.add(new SortedSetDocValuesField("level", new BytesRef("p")));
            doc.add(new TextField("abc", "document1", Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);


        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MIN));
//        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MIDDLE_MIN));
//        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MIDDLE_MAX));
//        Sort sortByLevel = new Sort(new SortedSetSortField("level", false, SortedSetSelector.Type.MAX));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("abc", "document1")), BooleanClause.Occur.MUST);
        TopDocs docs2 = searcher.search(builder.build(), 1000 , sortByLevel);


        System.out.println("sort by level");
        for (ScoreDoc scoreDoc: docs2.scoreDocs){
            System.out.println("docId: 文档"+ scoreDoc.doc+"");
        }
    }

    public static void main(String[] args) throws Exception{
        SortedSetDocValuesTest test = new SortedSetDocValuesTest();
        test.doIndexAndSearch();
    }
}
