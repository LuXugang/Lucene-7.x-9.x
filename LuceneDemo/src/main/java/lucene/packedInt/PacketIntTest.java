package lucene.packedInt;

import io.FileOperation;
import lucene.docValues.SortedDocValuesTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2019-04-02 21:02
 */
public class PacketIntTest {
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

        String groupField = "superStart";

        int count = 0;
        while (count++ < 130){
            Document doc = new Document();
            doc.add(new SortedDocValuesField(groupField, new BytesRef(getString(count))));
            indexWriter.addDocument(doc);
        }


        indexWriter.commit();
    }

    private String getString(int i){
       String string = "abtabcabrabt abfabyabrabxabm abuabeabqabv abhabdabuaba abmabvabaabu ablabhabh abqabcabiabw abmabjabiaby aboabuabtabwabm absabuabrabyabw abyabrabc abhabeabwabl " +
               "abgabtabcabg abvabdabeabx abnabtabb abyabmablablabt abrabbabuabjabp abcabbabnabu abuabjabfabm abqabmaboabrabv abpablabv abiabmabeabdabc abbabgabtabmabe " +
               "abwabuabyabiabh abuabyablabxabk absabwabxabbabg abqabtabf abmabaabiabc abeabiabhabrabd abuabsabp abhablabp ablabmabeabr abaabmabbabv abiabhabyabiabl abjabpabaabp abwabiaby absabrabtabx aboabkabg " +
               "aboabmaba ablabraby abuabyabyabyabc abqabkabcabfabi abhabgabt abrabjabpaby abxabeabs abyabaabm abjabxabl abgabjaba abeabwabq abcabqabx" +
               "kbakbokbwkbnkbx kbrkbbkbwkblkbj kbwkbrkbg kbkkbgkbk kbmkbdkbk kbnkbbkbl kbykbmkblkbw kbfkbnkbrkbn kbakbkkbpkbq kbnkbkkba kbxkbkkbikbr kbakbokbykbqkbp kbskbgkbk kbqkbxkbrkbwkbs kbykbokbbkbqkbc " +
               "kbfkbdkbt kbdkbtkbokbnkbu kbykbbkbf kblkbvkbh kbfkbukbl kbekbrkbikby kbhkbtkbt kbekbxkbn " +
               "kbukbxkblkbqkbj kbwkbwkbukbe kbkkbqkbbkbx kbhkbrkbvkbu kbykbtkbj kbgkbbkbckby kblkbskby kbpkbgkbrkbrkbx kbskbekbx kbekbokbb kbykbpkbk kbfkbfkbl " +
               "kbckbqkbf kbdkbckblkbqkby kbrkbbkbmkby kbpkbjkbt kbukbkkbtkbm kbikbhkbvkbmkbp kbjkbmkbt kbqkbvkbmkbp kbhkbkkbckbo kbrkbykbm kbckbbkbbkbj kbekbqkbqkbqkbd kbckbnkbr kbxkbbkbwkbr " +
               "kbqkbskbl kbckbqkbekbekbp kbskbnkbr kbtkbjkbskbwkbg kbkkbwkbikba kbmkbwkbdkbvkbi kbvkbxkbu kbwkbokbvkbh kbvkbjkbmkbn kbhkbukbx kbskbnkbtkbpkbv" +
               "cbclcbckco cbcbcycxcd cycicqcs ckcscscccj ciclcm cbcwcccd cnclcscj cdcocscect cscacmco cdcmcr cgcicjccct cackcm cqcyck cvcpclcack cuclcbctcu cocicicv cjcich " +
               "cxcxcw cpcicwcs cvcgcf cscrco cicqcnca cucrcacocs cfcqcrcc cmcvcv cecgcn chctcg clckcscacv cychci clcpcbcrcq";
       String[] stringArray = StringUtils.split(string, " ");
       return stringArray[i];
    }

    public static void main(String[] args) throws Exception{
        PacketIntTest test = new PacketIntTest();
        test.doSearch();
    }
}
