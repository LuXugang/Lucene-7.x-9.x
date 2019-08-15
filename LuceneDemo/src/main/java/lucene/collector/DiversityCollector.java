package lucene.collector;

import io.FileOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lu Xugang
 * @date 2019-08-15 21:17
 */
public class DiversityCollector {
    private static Directory dir;

    static {
        try {
            FileOperation.deleteFile("./data");
            dir = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static IndexReader reader;

    static class Record {
        String year;
        String artist;
        String song;
        float weeks;
        String id;

        public Record(String id, String year, String artist, String song,
                      float weeks) {
            super();
            this.id = id;
            this.year = year;
            this.artist = artist;
            this.song = song;
            this.weeks = weeks;
        }
    }

        private static String[] hitsOfThe60s = {
                "1966\tSPENCER DAVIS GROUP\tKEEP ON RUNNING\t1",
                "1966\tOVERLANDERS\tMICHELLE\t3",
                "1966\tNANCY SINATRA\tTHESE BOOTS ARE MADE FOR WALKIN'\t4",
                "1966\tWALKER BROTHERS\tTHE SUN AIN'T GONNA SHINE ANYMORE\t4",
                "1966\tSPENCER DAVIS GROUP\tSOMEBODY HELP ME\t2",
                "1966\tDUSTY SPRINGFIELD\tYOU DON'T HAVE TO SAY YOU LOVE ME\t1",
                "1966\tMANFRED MANN\tPRETTY FLAMINGO\t3",
                "1966\tROLLING STONES\tPAINT IT, BLACK\t1",
                "1966\tFRANK SINATRA\tSTRANGERS IN THE NIGHT\t3",
                "1966\tBEATLES\tPAPERBACK WRITER\t5",
                "1966\tKINKS\tSUNNY AFTERNOON\t2",
                "1966\tGEORGIE FAME AND THE BLUE FLAMES\tGETAWAY\t1",
                "1966\tCHRIS FARLOWE\tOUT OF TIME\t1",
                "1966\tTROGGS\tWITH A GIRL LIKE YOU\t2",
                "1966\tBEATLES\tYELLOW SUBMARINE/ELEANOR RIGBY\t4",
                "1966\tSMALL FACES\tALL OR NOTHING\t1",
                "1966\tJIM REEVES\tDISTANT DRUMS\t5",
                "1966\tFOUR TOPS\tREACH OUT I'LL BE THERE\t3",
                "1966\tBEACH BOYS\tGOOD VIBRATIONS\t2",
                "1966\tTOM JONES\tGREEN GREEN GRASS OF HOME\t4",
                "1967\tMONKEES\tI'M A BELIEVER\t4",
                "1967\tPETULA CLARK\tTHIS IS MY SONG\t2",
                "1967\tENGELBERT HUMPERDINCK\tRELEASE ME\t4",
                "1967\tNANCY SINATRA AND FRANK SINATRA\tSOMETHIN' STUPID\t2",
                "1967\tSANDIE SHAW\tPUPPET ON A STRING\t3",
                "1967\tTREMELOES\tSILENCE IS GOLDEN\t3",
                "1967\tPROCOL HARUM\tA WHITER SHADE OF PALE\t4",
                "1967\tBEATLES\tALL YOU NEED IS LOVE\t7",
                "1967\tSCOTT MCKENZIE\tSAN FRANCISCO (BE SURE TO WEAR SOME FLOWERS INYOUR HAIR)\t4",
                "1967\tENGELBERT HUMPERDINCK\tTHE LAST WALTZ\t5",
                "1967\tBEE GEES\tMASSACHUSETTS (THE LIGHTS WENT OUT IN)\t4",
                "1967\tFOUNDATIONS\tBABY NOW THAT I'VE FOUND YOU\t2",
                "1967\tLONG JOHN BALDRY\tLET THE HEARTACHES BEGIN\t2",
                "1967\tBEATLES\tHELLO GOODBYE\t5",
                "1968\tGEORGIE FAME\tTHE BALLAD OF BONNIE AND CLYDE\t1",
                "1968\tLOVE AFFAIR\tEVERLASTING LOVE\t2",
                "1968\tMANFRED MANN\tMIGHTY QUINN\t2",
                "1968\tESTHER AND ABI OFARIM\tCINDERELLA ROCKEFELLA\t3",
                "1968\tDAVE DEE, DOZY, BEAKY, MICK AND TICH\tTHE LEGEND OF XANADU\t1",
                "1968\tBEATLES\tLADY MADONNA\t2",
                "1968\tCLIFF RICHARD\tCONGRATULATIONS\t2",
                "1968\tLOUIS ARMSTRONG\tWHAT A WONDERFUL WORLD/CABARET\t4",
                "1968\tGARRY PUCKETT AND THE UNION GAP\tYOUNG GIRL\t4",
                "1968\tROLLING STONES\tJUMPING JACK FLASH\t2",
                "1968\tEQUALS\tBABY COME BACK\t3", "1968\tDES O'CONNOR\tI PRETEND\t1",
                "1968\tTOMMY JAMES AND THE SHONDELLS\tMONY MONY\t2",
                "1968\tCRAZY WORLD OF ARTHUR BROWN\tFIRE!\t1",
                "1968\tTOMMY JAMES AND THE SHONDELLS\tMONY MONY\t1",
                "1968\tBEACH BOYS\tDO IT AGAIN\t1",
                "1968\tBEE GEES\tI'VE GOTTA GET A MESSAGE TO YOU\t1",
                "1968\tBEATLES\tHEY JUDE\t8",
                "1968\tMARY HOPKIN\tTHOSE WERE THE DAYS\t6",
                "1968\tJOE COCKER\tWITH A LITTLE HELP FROM MY FRIENDS\t1",
                "1968\tHUGO MONTENEGRO\tTHE GOOD THE BAD AND THE UGLY\t4",
                "1968\tSCAFFOLD\tLILY THE PINK\t3",
                "1969\tMARMALADE\tOB-LA-DI, OB-LA-DA\t1",
                "1969\tSCAFFOLD\tLILY THE PINK\t1",
                "1969\tMARMALADE\tOB-LA-DI, OB-LA-DA\t2",
                "1969\tFLEETWOOD MAC\tALBATROSS\t1", "1969\tMOVE\tBLACKBERRY WAY\t1",
                "1969\tAMEN CORNER\t(IF PARADISE IS) HALF AS NICE\t2",
                "1969\tPETER SARSTEDT\tWHERE DO YOU GO TO (MY LOVELY)\t4",
                "1969\tMARVIN GAYE\tI HEARD IT THROUGH THE GRAPEVINE\t3",
                "1969\tDESMOND DEKKER AND THE ACES\tTHE ISRAELITES\t1",
                "1969\tBEATLES\tGET BACK\t6", "1969\tTOMMY ROE\tDIZZY\t1",
                "1969\tBEATLES\tTHE BALLAD OF JOHN AND YOKO\t3",
                "1969\tTHUNDERCLAP NEWMAN\tSOMETHING IN THE AIR\t3",
                "1969\tROLLING STONES\tHONKY TONK WOMEN\t5",
                "1969\tZAGER AND EVANS\tIN THE YEAR 2525 (EXORDIUM AND TERMINUS)\t3",
                "1969\tCREEDENCE CLEARWATER REVIVAL\tBAD MOON RISING\t3",
                "1969\tJANE BIRKIN AND SERGE GAINSBOURG\tJE T'AIME... MOI NON PLUS\t1",
                "1969\tBOBBIE GENTRY\tI'LL NEVER FALL IN LOVE AGAIN\t1",
                "1969\tARCHIES\tSUGAR, SUGAR\t4"};

        private static final Map<String, Record> parsedRecords = new HashMap<String, Record>();

        private static IndexWriter writer;

        static void doIndex() throws Exception {
            Analyzer analyzer = new WhitespaceAnalyzer();
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            conf.setUseCompoundFile(false);

            writer = new IndexWriter(dir, conf);
            // populate an index with documents - artist, song and weeksAtNumberOne
            Document doc = new Document();

            Field yearField = new TextField("year", "", Field.Store.NO);
            SortedDocValuesField artistField = new SortedDocValuesField("artist",
                    new BytesRef(""));
            Field weeksAtNumberOneField = new FloatDocValuesField("weeksAtNumberOne",
                    0.0F);
            Field weeksStoredField = new StoredField("weeks", 0.0F);
            Field idField = new TextField("id", "", Field.Store.YES);
            Field songField = new TextField("song", "", Field.Store.NO);
            Field storedArtistField = new TextField("artistName", "", Field.Store.NO);

            doc.add(idField);
            doc.add(weeksAtNumberOneField);
            doc.add(storedArtistField);
            doc.add(songField);
            doc.add(weeksStoredField);
            doc.add(yearField);
            doc.add(artistField);

            parsedRecords.clear();
            for (int i = 0; i < hitsOfThe60s.length; i++) {
                String cols[] = hitsOfThe60s[i].split("\t");
                Record record = new Record(String.valueOf(i), cols[0], cols[1], cols[2],
                        Float.parseFloat(cols[3]));
                parsedRecords.put(record.id, record);
                idField.setStringValue(record.id);
                yearField.setStringValue(record.year);
                storedArtistField.setStringValue(record.artist);
                artistField.setBytesValue(new BytesRef(record.artist));
                songField.setStringValue(record.song);
                weeksStoredField.setFloatValue(record.weeks);
                weeksAtNumberOneField.setFloatValue(record.weeks);
                writer.addDocument(doc);
                if (i % 10 == 0) {
                    // Causes the creation of multiple segments for our test
                    writer.commit();
                }
            }
            reader = DirectoryReader.open(writer);
            writer.close();
        }


    public static void main(String[] args) throws Exception{
            doIndex();
    }
}
