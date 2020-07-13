package io.util;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.*;

/**
 * @author Lu Xugang
 * @date 2020/7/10 11:02 上午
 */
public class FSTTest {
    public static void main(String[] args) throws Exception{
        String[] inputValues = {"mop", "moth", "pop", "star", "stop", "top"};
        long[] outputValues = {100, 91, 72, 83, 54, 55};
        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        Builder<Long> builder = new Builder<>(FST.INPUT_TYPE.BYTE1, outputs);
        IntsRefBuilder scratchInts = new IntsRefBuilder();
        for (int i = 0; i < inputValues.length; i++) {
            builder.add(Util.toIntsRef(new BytesRef(inputValues[i]), scratchInts), outputValues[i]);
        }
        FST<Long> fst = builder.finish();
        IntsRefFSTEnum<Long> intsRefFSTEnum = new IntsRefFSTEnum<>(fst);
        BytesRefBuilder builder1 = new BytesRefBuilder();
        while (intsRefFSTEnum.next() != null){
            IntsRefFSTEnum.InputOutput<Long> inputOutput = intsRefFSTEnum.current();
            BytesRef bytesRef = Util.toBytesRef(inputOutput.input, builder1);
            System.out.println(bytesRef.utf8ToString() + ":" + inputOutput.output);

        }
    }
}
