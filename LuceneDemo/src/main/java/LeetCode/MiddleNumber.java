package LeetCode;

/**
 * @author Lu Xugang
 * @date 2019/10/18 4:13 下午
 */
public class MiddleNumber {

    public void doJob(){
        int num1[] = {1, 3, 4, 6 ,8};
        int num2[] = {2, 3, 8, 10 ,11};
        int total = num1.length + num2.length;
        int num1Index = 0;
        int num2Index = 0;

        int count = 0;
        int middleIndex = total / 2;
        while (count++ < total){
            if(num1Index + num1Index == middleIndex){
                System.out.println("num1Index: "+num1Index+"");
                System.out.println("num2Index: "+num2Index+"");
                break;
            }
            if(num1[num1Index] >= num2[num2Index]){
                num2Index++;
            }else {
                num1Index++;
            }
        }

    }

    public static void main(String[] args) {
        MiddleNumber middleNumber = new MiddleNumber();
        middleNumber.doJob();
    }

}
