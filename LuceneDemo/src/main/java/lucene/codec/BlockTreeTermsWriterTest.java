package lucene.codec;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BlockTreeTermsWriterTest {
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
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        Document doc;
        // 0

         String fieldValues1 = getMultiSamePrefixValue("ab", 100, 4);
        String fieldValues22= getMultiSamePrefixValue("abg", 28, 4);
        String sortedFieldValues = getSortedValues(fieldValues1);
//         String []fieldValuesArray = sortedFieldValues.split(",");
//        String []fieldValuesArray = {"abc", "abd", "abe", "abea", "abeb", "abee"};
        String [] fieldValuesArray = {"aa","ab", "abeabsabv",
                "abg", "abga", "abgaabgr", "abgc", "abgc", "abgdabgg", "abgdabgj", "abgfabgj", "abgg", "abggabgjabgi", "abghabgiabga", "abgjabgmabge", "abgkabgbabge", "abgkabgqabgt", "abgkabgvabgi", "abgnabgqabgx", "abgnabgrabgi", "abgoabge", "abgoabgtabgf", "abgp", "abgpabgkabgm", "abgpabgnabgb", "abgrabgyabgw", "abgsabgeabgu", "abguabga", "abgvabgjabgo", "abgy", "abgyabgaabgk",
                "abh", "abi", "abiabg",
                "abiabyabj", "abj", "abjabkaba", "ablabpabr", "ablabqabu", "abm", "abmabd", "abnabeabd", "abo",
                "aboa", "abob", "aboabqaba", "abpabfabi", "abrabt", "abs", "absabrabi", "abtaboabx", "abuabp",
                "abvabdabn", "abvabw", "abw", "abwabtabf", "end"};
        StringBuilder builder = new StringBuilder();
        for (String s : fieldValuesArray) {
           builder.append(s);
            builder.append(" ");
        }
        String fieldValues = builder.toString();


        int count = 0;
        while (count++ < 1) {
            doc = new Document();
            doc.add(new Field("content", fieldValues, type));
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
    }

    public static String getSortedValues(String source){
       String [] sourceArray = source.split(" ");
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(sourceArray));
        return Arrays.toString((list.stream().sorted()).toArray());
    }



    public static String getMultiSamePrefixValue(String prefix, int wordNum, int maxWordLength) {
        int valueCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        while (valueCount++ < wordNum){
            stringBuilder.append(getSamePrefixRandomValue(prefix, maxWordLength));
            stringBuilder.append(" ");
        }
        stringBuilder.append("end");
        return stringBuilder.toString();
    }

    public static String getSamePrefixRandomValue(String prefix, int maxWordLength){
        return getString(prefix, getLength(maxWordLength));
    }

    public static String getString(String prefix, int length) {
        String str="abcdefghijklmnopqrstuvwxyz";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(25);
            sb.append(prefix);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static int getLength(int maxWordLength) {
        Random random = new Random();
        int length = random.nextInt(maxWordLength);
        if (length < 3) {
            length = length + 1;
        }
        return length;
    }

    public static void main(String[] args) throws Throwable{
        BlockTreeTermsWriterTest termsWrtierTest = new BlockTreeTermsWriterTest();
        termsWrtierTest.doSearch();
    }
}
