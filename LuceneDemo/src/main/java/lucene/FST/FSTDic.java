package lucene.FST;

import org.apache.lucene.util.*;
import org.apache.lucene.util.fst.*;
import org.apache.lucene.util.fst.FST;

/**
 * @author Lu Xugang
 * @date 2019-02-15 10:22
 */
public class FSTDic {
  public static void main(String[] args) throws Exception{
    String[] inputValues = {"mop", "moth", "pop", "star", "stop", "top"};
      long[] outputValues = {90, 91, 92, 93, 94, 95};
      PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
      Builder<Long> builder = new Builder<>(FST.INPUT_TYPE.BYTE1, outputs);
      IntsRefBuilder scratchInts = new IntsRefBuilder();
      for (int i = 0; i < inputValues.length; i++) {
        builder.add(Util.toIntsRef(new BytesRef(inputValues[i]), scratchInts), outputValues[i]);
      }
      FST<Long> fst = builder.finish();

      Long value = Util.get(fst, new BytesRef("stop"));
      System.out.println(value); // 4
  }
}
