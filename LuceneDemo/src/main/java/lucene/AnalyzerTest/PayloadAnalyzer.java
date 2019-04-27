package lucene.AnalyzerTest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PayloadAnalyzer extends Analyzer {

    Map<String,PayloadData> fieldToData = new HashMap<>();

    public PayloadAnalyzer() {
        super(PER_FIELD_REUSE_STRATEGY);
    }

    public PayloadAnalyzer(String field, byte[] data, int offset, int length) {
        super(PER_FIELD_REUSE_STRATEGY);
        setPayloadData(field, data, offset, length);
    }

    public void setPayloadData(String field, byte[] data, int offset, int length) {
        fieldToData.put(field, new PayloadData(data, offset, length));
    }

    @Override
    public TokenStreamComponents createComponents(String fieldName) {
        PayloadData payload =  fieldToData.get(fieldName);
        Tokenizer ts = new WhitespaceTokenizer();
        TokenStream tokenStream = (payload != null) ?
                new PayloadFilter(ts, fieldName, fieldToData) : ts;
        return new TokenStreamComponents(ts, tokenStream);
    }

    static class PayloadData {
        byte[] data;
        int offset;
        int length;

        PayloadData(byte[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
        }
    }

    private static class PayloadFilter extends TokenFilter {
        PayloadAttribute payloadAtt;
        CharTermAttribute termAttribute;
        private Map<String,PayloadData> fieldToData;
        private String fieldName;
        private PayloadData payloadData;
        private int offset;

        public PayloadFilter(TokenStream in, String fieldName, Map<String,PayloadData> fieldToData) {
            super(in);
            this.fieldToData = fieldToData;
            this.fieldName = fieldName;
            payloadAtt = addAttribute(PayloadAttribute.class);
            termAttribute = addAttribute(CharTermAttribute.class);
        }

        @Override
        public boolean incrementToken() throws IOException {
            boolean hasNext = input.incrementToken();
            if (!hasNext) {
                return false;
            }

            // Some values of the same field are to have payloads and others not
            if (termAttribute.toString().endsWith("book")) {
                BytesRef p = new BytesRef(payloadData.data, offset, payloadData.length);
                payloadAtt.setPayload(p);
            } else {
                payloadAtt.setPayload(null);
            }

            return true;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            this.payloadData = fieldToData.get(fieldName);
            this.offset = payloadData.offset;
        }
    }


}
