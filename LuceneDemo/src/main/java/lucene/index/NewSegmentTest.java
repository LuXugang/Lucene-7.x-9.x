package lucene.index;

/**
 * @author Lu Xugang
 * @date 2019/10/25 11:33 上午
 */
public class NewSegmentTest {

    private static void getNewSegmentPrefixName(){
        int total = 0 ;
        int count = 0;
        System.out.print("newSegmentPrefixName: ");
        while (count++ < 80){
                System.out.print("_"+Long.toString(count, Character.MAX_RADIX)+"");
                System.out.print(" ");
                if(total++ > 40){
                    total = total - 10000;
                    System.out.println(" ");
                }
        }
    }

    public static void main(String[] args) {
        getNewSegmentPrefixName();
    }
}
