package lucene.index;

/**
 * @author Lu Xugang
 * @date 2019/10/25 11:33 上午
 */
public class NewSegmentTest {

    private static void doJob(){
        int count = 0;
        while (count++ < 100){
            System.out.println("newSegmentPrefix: "+Long.toString(count, Character.MAX_RADIX)+"");
        }
    }

    public static void main(String[] args) {
        doJob();
    }
}
