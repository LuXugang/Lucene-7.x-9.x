package lucene.compress.DirectWriterDirectReader;

import io.FileOperation;
import org.apache.lucene.store.*;
import org.apache.lucene.util.LongValues;
import org.apache.lucene.util.packed.DirectReader;
import org.apache.lucene.util.packed.DirectWriter;

import java.nio.file.Paths;
import java.util.Random;

/**
 * @author Lu Xugang
 * @date 2019/10/10 7:20 下午
 */
public class DirectWriterDirectReaderTest {
    private Directory dir ;
    private int numberOfValues;
    private int maxValue;
    private int bitsPerValue;

    public DirectWriterDirectReaderTest(int numberOfValues, int maxValue) throws Exception{
        FileOperation.deleteFile("./data");
        dir = FSDirectory.open(Paths.get("/Users/luxugang/project/github/Lucene-7.5.0/LuceneDemo/data"));
        this.numberOfValues = numberOfValues;
        this.maxValue = maxValue;
        this.bitsPerValue = DirectWriter.bitsRequired(1L << 8);
    }


    void doWriter()throws Exception{
        Random random = new Random();
        IndexOutput output = dir.createOutput("packed", IOContext.DEFAULT);
        DirectWriter writer = DirectWriter.getInstance(output, numberOfValues, bitsPerValue);
        System.out.print("input: ");
        for (int i = 0; i < numberOfValues; i++) {
            long randomValue = (1L << 8) + i;
            System.out.print(""+randomValue+" ");
            writer.add(randomValue);
        }
        writer.finish();
        output.close();
        System.out.println(" ");
    }
    void doReader()throws Exception{

        IndexInput in = dir.openInput("packed", IOContext.DEFAULT);
        LongValues values = DirectReader.getInstance(in.randomAccessSlice(0, in.length()), bitsPerValue);
        System.out.print("output: ");
        for (int i = 0; i < numberOfValues; i++) {
            long value = values.get(i);
            System.out.print(""+value+" ");
        }
        System.out.println(" ");
    }
    public static void main(String[] args) throws Exception{
        DirectWriterDirectReaderTest test = new DirectWriterDirectReaderTest(4, 10);
        test.doWriter();
        test.doReader();
    }
}
