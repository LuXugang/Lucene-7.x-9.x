public class TestCount {
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
    private IndexWriter indexWriter;

    public void doSearch() throws Exception {
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        int count = 0;
        Document doc ;
        while (count++ < 10000){
            doc = new Document();
            doc.add(new TextField("author",  "ably lily baby andy lucy ably", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new TextField("author",  "lily and tom for you", Field.Store.YES));
            indexWriter.addDocument(doc);

            doc = new Document();
            doc.add(new TextField("author",  "i love you good", Field.Store.YES));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("author", "lily")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("author", "you")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("author", "good")), BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(1);
        TopDocs docs = searcher.search(builder.build(), 5);
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            System.out.println("socre: " + scoreDoc.score + " doc" + scoreDoc.doc);
        }

        System.out.println("abc");

    }

    public static void main(String[] args) throws Exception{
        TestMaxScore test = new TestMaxScore();
        test.doSearch();
    }
}
