package io.index;

import io.util.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.codecs.*;
import org.apache.lucene.codecs.lucene50.Lucene50CompoundFormat;
import org.apache.lucene.codecs.lucene50.Lucene50LiveDocsFormat;
import org.apache.lucene.codecs.lucene50.Lucene50TermVectorsFormat;
import org.apache.lucene.codecs.lucene60.Lucene60FieldInfosFormat;
import org.apache.lucene.codecs.lucene80.Lucene80NormsFormat;
import org.apache.lucene.codecs.lucene86.Lucene86PointsFormat;
import org.apache.lucene.codecs.lucene86.Lucene86SegmentInfoFormat;
import org.apache.lucene.codecs.lucene87.Lucene87StoredFieldsFormat;
import org.apache.lucene.codecs.perfield.PerFieldDocValuesFormat;
import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat;
import org.apache.lucene.codecs.uniformsplit.sharedterms.STUniformSplitPostingsFormat;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2020/12/25 14:36
 */
public class UniformSplitTermsTest {
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
        conf.setReaderPooling(false);
        Codec codec = new NewCodec();
        conf.setMergeScheduler(new SerialMergeScheduler());
        conf.setReaderPooling(true);
        conf.setCodec(codec);
        String sortedField = "oldSorterRule";
        String sortedField2 = "newSorterRule";
        SortField indexSortField = new SortField(sortedField, SortField.Type.LONG);
        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);
        FieldType type = new FieldType();
        type.setStored(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorPayloads(true);
        FieldType type1 = new FieldType();
        type1.setStored(true);
        type1.setTokenized(true);
        type1.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        Document doc ;
        int commitCount = 0;
        while (commitCount++ < 1){
            int count = 0;
//            while (count++ < 1000){

                doc = new Document();
//                doc.add(new Field("content", getRandomString(new Random().nextInt(6)), type1));
                doc.add(new Field("content", "cd", type1));
                doc.add(new IntPoint("intField", 3, 4));
                indexWriter.addDocument(doc);
                // 文档1
                doc = new Document();
                doc.add(new NumericDocValuesField(sortedField, 3));
                doc.add(new NumericDocValuesField(sortedField2, 1));
                doc.add(new IntPoint("intField", 7, 4));
                indexWriter.addDocument(doc);
                // 文档2
                doc = new Document();
                doc.add(new Field("content", "the name is name", type1));
                doc.add(new NumericDocValuesField(sortedField, 2));
                doc.add(new IntPoint("intField", new Random().nextInt(100), 4));
                indexWriter.addDocument(doc);
//            }
            indexWriter.commit();
        }
        indexWriter.commit();
        System.out.println("ab");
    }
    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyz";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(26);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception{
        UniformSplitTermsTest test = new UniformSplitTermsTest();
        test.doIndexAndSearch();
    }

    public static class NewCodec extends Codec{
        public NewCodec() {
            this(Lucene87StoredFieldsFormat.Mode.BEST_SPEED);
        }

        public NewCodec(Lucene87StoredFieldsFormat.Mode mode) {
            super("Lucene87");
            this.storedFieldsFormat = new Lucene87StoredFieldsFormat(Objects.requireNonNull(mode));
            this.defaultFormat = new STUniformSplitPostingsFormat();
        }

        private final TermVectorsFormat vectorsFormat = new Lucene50TermVectorsFormat();
        private final FieldInfosFormat fieldInfosFormat = new Lucene60FieldInfosFormat();
        private final SegmentInfoFormat segmentInfosFormat = new Lucene86SegmentInfoFormat();
        private final LiveDocsFormat liveDocsFormat = new Lucene50LiveDocsFormat();
        private final CompoundFormat compoundFormat = new Lucene50CompoundFormat();
        private final PointsFormat pointsFormat = new Lucene86PointsFormat();
        private final PostingsFormat defaultFormat;

        private final PostingsFormat postingsFormat = new PerFieldPostingsFormat() {
            @Override
            public PostingsFormat getPostingsFormatForField(String field) {
                return NewCodec.this.getPostingsFormatForField(field);
            }
        };

        private final DocValuesFormat docValuesFormat = new PerFieldDocValuesFormat() {
            @Override
            public DocValuesFormat getDocValuesFormatForField(String field) {
                return NewCodec.this.getDocValuesFormatForField(field);
            }
        };

        private final StoredFieldsFormat storedFieldsFormat;

        @Override
        public final StoredFieldsFormat storedFieldsFormat() {
            return storedFieldsFormat;
        }

        @Override
        public final TermVectorsFormat termVectorsFormat() {
            return vectorsFormat;
        }

        @Override
        public final PostingsFormat postingsFormat() {
            return postingsFormat;
        }

        @Override
        public final FieldInfosFormat fieldInfosFormat() {
            return fieldInfosFormat;
        }

        @Override
        public final SegmentInfoFormat segmentInfoFormat() {
            return segmentInfosFormat;
        }

        @Override
        public final LiveDocsFormat liveDocsFormat() {
            return liveDocsFormat;
        }

        @Override
        public final CompoundFormat compoundFormat() {
            return compoundFormat;
        }

        @Override
        public final PointsFormat pointsFormat() {
            return pointsFormat;
        }
        public PostingsFormat getPostingsFormatForField(String field) {
            return defaultFormat;
        }
        @Override
        public final DocValuesFormat docValuesFormat() {
            return docValuesFormat;
        }
        @Override
        public final NormsFormat normsFormat() {
            return normsFormat;
        }

        public DocValuesFormat getDocValuesFormatForField(String field) {
            return defaultDVFormat;
        }

        private final DocValuesFormat defaultDVFormat = DocValuesFormat.forName("Lucene80");

        private final NormsFormat normsFormat = new Lucene80NormsFormat();
    }
}
