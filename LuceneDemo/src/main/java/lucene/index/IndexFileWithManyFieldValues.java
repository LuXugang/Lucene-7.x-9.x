package lucene.index;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019-02-21 09:58
 */
public class IndexFileWithManyFieldValues {
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

  public void doIndex() throws Exception {

    conf.setUseCompoundFile(false);
    LogMergePolicy policy = new LogDocMergePolicy();
    policy.setMergeFactor(5);
    conf.setMergePolicy(policy);
    indexWriter = new IndexWriter(directory, conf);
    int count = 0;
    int n = 0;
    boolean exchange = true;
    while (count++ < 7777096) {
//       0
      Document doc = new Document();
////    doc.add(new TextField("author", "aab b a aabbcc ", Field.Store.YES));
//    doc.add(new TextField("content", "a", Field.Store.YES));
//    indexWriter.addDocument(doc);
//
//    // 1
//    doc = new Document();
////    doc.add(new TextField("author", "cd a", Field.Store.YES));
//    doc.add(new TextField("content", "b", Field.Store.YES));
//    indexWriter.addDocument(doc);

      // 2
      doc = new Document();
//    doc.add(new TextField("author", "aab acb aab", Field.Store.YES));
      doc.add(new TextField("content", getRandomValue(), Field.Store.YES));
      indexWriter.addDocument(doc);

//    String abPrefixTerm = "abtabcabrabt abfabyabrabxabm abuabeabqabv abhabdabuaba abmabvabaabu ablabhabh abqabcabiabw abmabjabiaby aboabuabtabwabm absabuabrabyabw abyabrabc abhabeabwabl abgabtabcabg abvabdabeabx abnabtabb abyabmablablabt abrabbabuabjabp abcabbabnabu abuabjabfabm abqabmaboabrabv abpablabv abiabmabeabdabc abbabgabtabmabe abwabuabyabiabh abuabyablabxabk absabwabxabbabg abqabtabf abmabaabiabc abeabiabhabrabd abuabsabp abhablabp ablabmabeabr abaabmabbabv abiabhabyabiabl abjabpabaabp abwabiaby absabrabtabx aboabkabg aboabmaba ablabraby abuabyabyabyabc abqabkabcabfabi abhabgabt abrabjabpaby abxabeabs abyabaabm abjabxabl abgabjaba abeabwabq abcabqabx";
//    // 3
//    doc = new Document();
//    doc.add(new TextField("author", abPrefixTerm, Field.Store.YES));
//    doc.add(new TextField("content", "c", Field.Store.YES));
//    indexWriter.addDocument(doc);
//
//
//    String fixTerm = "dah dhinr gnvu nua ufg tvb krcdd nng snxk nqgs dwac pgjw jsdm bfanu shdq nkj psdfp eqc jsbxv vebe ouum vfi ejs kdrm oye fct exqd yyafa elq ted vml xbbdy vavg fdkts hjub squu cewwx shs ulex ibqt umr xree rdux hdm mcgr ach bveq vcyh tnx rxcxq gybn cgh xxd smte drkng uypde ufbq epcx cpek leje akt lmnjk ieu kvet bfc ytsvb uye dqdn nibr djok deay tbl dvy ulh jee ociu pvik wtpv xdrx isym ocl yxuyy wipp ead opw tywd prxmf tbq gbvy fpxq sth bcix klbg ifg fcav lepl pvq pdrdv uqw";
//    // 4
//    doc = new Document();
//    doc.add(new TextField("author", fixTerm, Field.Store.YES));
//    doc.add(new TextField("content", "c", Field.Store.YES));
//    indexWriter.addDocument(doc);
//
//    String kbPrefixTerm = "kbakbokbwkbnkbx kbrkbbkbwkblkbj kbwkbrkbg kbkkbgkbk kbmkbdkbk kbnkbbkbl kbykbmkblkbw kbfkbnkbrkbn kbakbkkbpkbq kbnkbkkba kbxkbkkbikbr kbakbokbykbqkbp kbskbgkbk kbqkbxkbrkbwkbs kbykbokbbkbqkbc kbfkbdkbt kbdkbtkbokbnkbu kbykbbkbf kblkbvkbh kbfkbukbl kbekbrkbikby kbhkbtkbt kbekbxkbn kbukbxkblkbqkbj kbwkbwkbukbe kbkkbqkbbkbx kbhkbrkbvkbu kbykbtkbj kbgkbbkbckby kblkbskby kbpkbgkbrkbrkbx kbskbekbx kbekbokbb kbykbpkbk kbfkbfkbl kbckbqkbf kbdkbckblkbqkby kbrkbbkbmkby kbpkbjkbt kbukbkkbtkbm kbikbhkbvkbmkbp kbjkbmkbt kbqkbvkbmkbp kbhkbkkbckbo kbrkbykbm kbckbbkbbkbj kbekbqkbqkbqkbd kbckbnkbr kbxkbbkbwkbr kbqkbskbl kbckbqkbekbekbp kbskbnkbr kbtkbjkbskbwkbg kbkkbwkbikba kbmkbwkbdkbvkbi kbvkbxkbu kbwkbokbvkbh kbvkbjkbmkbn kbhkbukbx kbskbnkbtkbpkbv";
//    // 5
//    doc = new Document();
//    doc.add(new TextField("author", kbPrefixTerm, Field.Store.YES));
//    doc.add(new TextField("content", "c", Field.Store.YES));
//    indexWriter.addDocument(doc);
      if(count % 800 == 0){
        if(n++ == 2){
          n = 0;
          continue;
        }
        indexWriter.flush();
      }
    }
    indexWriter.commit();

    DirectoryReader  reader = DirectoryReader.open(indexWriter);
    IndexSearcher searcher = new IndexSearcher(reader);
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
//    builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.MUST);
    builder.add(new TermQuery(new Term("content", "a")), BooleanClause.Occur.SHOULD);
    Query query = builder.build();


    TotalHitCountCollector collector = new TotalHitCountCollector();

    searcher.search(query, collector);

    Document document  = reader.document(2);
    System.out.println(document.get("content"));

    // Per-top-reader state:
  }

  public static String getSamePrefixRandomValue(String prefix){
    String str="abcdefghijklmnopqrstuvwxyz";
    Random random=new Random();
    StringBuffer sb=new StringBuffer();
    int length = getLength();
    for(int i=0;i<length;i++){
      int number=random.nextInt(25);
      sb.append(prefix);
      sb.append(str.charAt(number));
    }
    return sb.toString();
  }

  public static String getRandomValue(){
    String str="abcdefghijklmnopqrstuvwxyz";
    Random random=new Random();
    StringBuffer sb=new StringBuffer();
    int length = getLength();
    for(int i=0;i<length;i++){
      int number=random.nextInt(25);
      sb.append(str.charAt(number));
    }
    return sb.toString();
  }

  public static int getLength(){
    Random random = new Random();
    int length = random.nextInt(5);
    if (length < 3){
      length = length + 3;
    }
    return length;
  }

  public static String getMultiSamePrefixValue(String prefix, int wordNum){
    int valueCount = 0;
    StringBuilder stringBuilder = new StringBuilder();
    while (valueCount++ < wordNum){
      stringBuilder.append(getSamePrefixRandomValue(prefix));
      stringBuilder.append(" ");
    }
    stringBuilder.append("end");
    return stringBuilder.toString();
  }

  public static String getMultiValue(){
    int valueCount = 0;
    StringBuilder stringBuilder = new StringBuilder();
    while (valueCount++ < 99){
      stringBuilder.append(getRandomValue());
      stringBuilder.append(" ");
    }
    stringBuilder.append("end");
    return stringBuilder.toString();
  }

  public static void main(String[] args) throws Exception{
    IndexFileWithManyFieldValues test = new IndexFileWithManyFieldValues();
    test.doIndex();
  }
}
