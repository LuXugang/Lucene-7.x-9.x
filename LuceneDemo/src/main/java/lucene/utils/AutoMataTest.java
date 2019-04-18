package lucene.utils;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;

/**
 * @author Lu Xugang
 * @date 2019-04-16 22:12
 */
public class AutoMataTest {
  public static void main(String[] args) {
    Automata.makeBinaryInterval(new BytesRef("bc"), true, new BytesRef("gch"), true);
  }
}
