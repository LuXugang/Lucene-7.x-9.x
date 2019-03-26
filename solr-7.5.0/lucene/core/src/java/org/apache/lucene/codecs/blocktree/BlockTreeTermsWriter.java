/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.codecs.blocktree;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.codecs.BlockTermState;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

/*
  TODO:
  
    - Currently there is a one-to-one mapping of indexed
      term to term block, but we could decouple the two, ie,
      put more terms into the index than there are blocks.
      The index would take up more RAM but then it'd be able
      to avoid seeking more often and could make PK/FuzzyQ
      faster if the additional indexed terms could store
      the offset into the terms block.

    - The blocks are not written in true depth-first
      order, meaning if you just next() the file pointer will
      sometimes jump backwards.  For example, block foo* will
      be written before block f* because it finished before.
      This could possibly hurt performance if the terms dict is
      not hot, since OSs anticipate sequential file access.  We
      could fix the writer to re-order the blocks as a 2nd
      pass.

    - Each block encodes the term suffixes packed
      sequentially using a separate vInt per term, which is
      1) wasteful and 2) slow (must linear scan to find a
      particular suffix).  We should instead 1) make
      random-access array so we can directly access the Nth
      suffix, and 2) bulk-encode this array using bulk int[]
      codecs; then at search time we can binary search when
      we seek a particular term.
*/

/**
 * Block-based terms index and dictionary writer.
 * <p>
 * Writes terms dict and index, block-encoding (column
 * stride) each term's metadata for each set of terms
 * between two index terms.
 * <p>
 *
 * Files:
 * <ul>
 *   <li><tt>.tim</tt>: <a href="#Termdictionary">Term Dictionary</a></li>
 *   <li><tt>.tip</tt>: <a href="#Termindex">Term Index</a></li>
 * </ul>
 * <p>
 * <a name="Termdictionary"></a>
 * <h3>Term Dictionary</h3>
 *
 * <p>The .tim file contains the list of terms in each
 * field along with per-term statistics (such as docfreq)
 * and per-term metadata (typically pointers to the postings list
 * for that term in the inverted index).
 * </p>
 *
 * <p>The .tim is arranged in blocks: with blocks containing
 * a variable number of entries (by default 25-48), where
 * each entry is either a term or a reference to a
 * sub-block.</p>
 *
 * <p>NOTE: The term dictionary can plug into different postings implementations:
 * the postings writer/reader are actually responsible for encoding 
 * and decoding the Postings Metadata and Term Metadata sections.</p>
 *
 * <ul>
 *    <li>TermsDict (.tim) --&gt; Header, <i>PostingsHeader</i>, NodeBlock<sup>NumBlocks</sup>,
 *                               FieldSummary, DirOffset, Footer</li>
 *    <li>NodeBlock --&gt; (OuterNode | InnerNode)</li>
 *    <li>OuterNode --&gt; EntryCount, SuffixLength, Byte<sup>SuffixLength</sup>, StatsLength, &lt; TermStats &gt;<sup>EntryCount</sup>, MetaLength, &lt;<i>TermMetadata</i>&gt;<sup>EntryCount</sup></li>
 *    <li>InnerNode --&gt; EntryCount, SuffixLength[,Sub?], Byte<sup>SuffixLength</sup>, StatsLength, &lt; TermStats ? &gt;<sup>EntryCount</sup>, MetaLength, &lt;<i>TermMetadata ? </i>&gt;<sup>EntryCount</sup></li>
 *    <li>TermStats --&gt; DocFreq, TotalTermFreq </li>
 *    <li>FieldSummary --&gt; NumFields, &lt;FieldNumber, NumTerms, RootCodeLength, Byte<sup>RootCodeLength</sup>,
 *                            SumTotalTermFreq?, SumDocFreq, DocCount, LongsSize, MinTerm, MaxTerm&gt;<sup>NumFields</sup></li>
 *    <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
 *    <li>DirOffset --&gt; {@link DataOutput#writeLong Uint64}</li>
 *    <li>MinTerm,MaxTerm --&gt; {@link DataOutput#writeVInt VInt} length followed by the byte[]</li>
 *    <li>EntryCount,SuffixLength,StatsLength,DocFreq,MetaLength,NumFields,
 *        FieldNumber,RootCodeLength,DocCount,LongsSize --&gt; {@link DataOutput#writeVInt VInt}</li>
 *    <li>TotalTermFreq,NumTerms,SumTotalTermFreq,SumDocFreq --&gt; 
 *        {@link DataOutput#writeVLong VLong}</li>
 *    <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}</li>
 * </ul>
 * <p>Notes:</p>
 * <ul>
 *    <li>Header is a {@link CodecUtil#writeHeader CodecHeader} storing the version information
 *        for the BlockTree implementation.</li>
 *    <li>DirOffset is a pointer to the FieldSummary section.</li>
 *    <li>DocFreq is the count of documents which contain the term.</li>
 *    <li>TotalTermFreq is the total number of occurrences of the term. This is encoded
 *        as the difference between the total number of occurrences and the DocFreq.</li>
 *    <li>FieldNumber is the fields number from {@link FieldInfos}. (.fnm)</li>
 *    <li>NumTerms is the number of unique terms for the field.</li>
 *    <li>RootCode points to the root block for the field.</li>
 *    <li>SumDocFreq is the total number of postings, the number of term-document pairs across
 *        the entire field.</li>
 *    <li>DocCount is the number of documents that have at least one posting for this field.</li>
 *    <li>LongsSize records how many long values the postings writer/reader record per term
 *        (e.g., to hold freq/prox/doc file offsets).
 *    <li>MinTerm, MaxTerm are the lowest and highest term in this field.</li>
 *    <li>PostingsHeader and TermMetadata are plugged into by the specific postings implementation:
 *        these contain arbitrary per-file data (such as parameters or versioning information) 
 *        and per-term data (such as pointers to inverted files).</li>
 *    <li>For inner nodes of the tree, every entry will steal one bit to mark whether it points
 *        to child nodes(sub-block). If so, the corresponding TermStats and TermMetaData are omitted </li>
 * </ul>
 * <a name="Termindex"></a>
 * <h3>Term Index</h3>
 * <p>The .tip file contains an index into the term dictionary, so that it can be 
 * accessed randomly.  The index is also used to determine
 * when a given term cannot exist on disk (in the .tim file), saving a disk seek.</p>
 * <ul>
 *   <li>TermsIndex (.tip) --&gt; Header, FSTIndex<sup>NumFields</sup>
 *                                &lt;IndexStartFP&gt;<sup>NumFields</sup>, DirOffset, Footer</li>
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
 *   <li>DirOffset --&gt; {@link DataOutput#writeLong Uint64}</li>
 *   <li>IndexStartFP --&gt; {@link DataOutput#writeVLong VLong}</li>
 *   <!-- TODO: better describe FST output here -->
 *   <li>FSTIndex --&gt; {@link FST FST&lt;byte[]&gt;}</li>
 *   <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}</li>
 * </ul>
 * <p>Notes:</p>
 * <ul>
 *   <li>The .tip file contains a separate FST for each
 *       field.  The FST maps a term prefix to the on-disk
 *       block that holds all terms starting with that
 *       prefix.  Each field's IndexStartFP points to its
 *       FST.</li>
 *   <li>DirOffset is a pointer to the start of the IndexStartFPs
 *       for all fields</li>
 *   <li>It's possible that an on-disk block would contain
 *       too many terms (more than the allowed maximum
 *       (default: 48)).  When this happens, the block is
 *       sub-divided into new blocks (called "floor
 *       blocks"), and then the output in the FST for the
 *       block's prefix encodes the leading byte of each
 *       sub-block, and its file pointer.
 * </ul>
 *
 * @see BlockTreeTermsReader
 * @lucene.experimental
 */
public final class BlockTreeTermsWriter extends FieldsConsumer {

  /** Suggested default value for the {@code
   *  minItemsInBlock} parameter to {@link
   *  #BlockTreeTermsWriter(SegmentWriteState,PostingsWriterBase,int,int)}. */
  public final static int DEFAULT_MIN_BLOCK_SIZE = 25;

  /** Suggested default value for the {@code
   *  maxItemsInBlock} parameter to {@link
   *  #BlockTreeTermsWriter(SegmentWriteState,PostingsWriterBase,int,int)}. */
  public final static int DEFAULT_MAX_BLOCK_SIZE = 48;

  //public static boolean DEBUG = false;
  //public static boolean DEBUG2 = false;

  //private final static boolean SAVE_DOT_FILES = false;

  private final IndexOutput termsOut;
  private final IndexOutput indexOut;
  final int maxDoc;
  final int minItemsInBlock;
  final int maxItemsInBlock;

  final PostingsWriterBase postingsWriter;
  final FieldInfos fieldInfos;

  private static class FieldMetaData {
    public final FieldInfo fieldInfo;
    public final BytesRef rootCode;
    public final long numTerms;
    public final long indexStartFP;
    public final long sumTotalTermFreq;
    public final long sumDocFreq;
    public final int docCount;
    private final int longsSize;
    public final BytesRef minTerm;
    public final BytesRef maxTerm;

    public FieldMetaData(FieldInfo fieldInfo, BytesRef rootCode, long numTerms, long indexStartFP, long sumTotalTermFreq, long sumDocFreq, int docCount, int longsSize,
                         BytesRef minTerm, BytesRef maxTerm) {
      assert numTerms > 0;
      this.fieldInfo = fieldInfo;
      assert rootCode != null: "field=" + fieldInfo.name + " numTerms=" + numTerms;
      this.rootCode = rootCode;
      this.indexStartFP = indexStartFP;
      this.numTerms = numTerms;
      this.sumTotalTermFreq = sumTotalTermFreq;
      this.sumDocFreq = sumDocFreq;
      this.docCount = docCount;
      this.longsSize = longsSize;
      this.minTerm = minTerm;
      this.maxTerm = maxTerm;
    }
  }

  private final List<FieldMetaData> fields = new ArrayList<>();

  /** Create a new writer.  The number of items (terms or
   *  sub-blocks) per block will aim to be between
   *  minItemsPerBlock and maxItemsPerBlock, though in some
   *  cases the blocks may be smaller than the min. */
  public BlockTreeTermsWriter(SegmentWriteState state,
                              PostingsWriterBase postingsWriter,
                              int minItemsInBlock,
                              int maxItemsInBlock)
    throws IOException
  {
    validateSettings(minItemsInBlock,
                     maxItemsInBlock);

    this.minItemsInBlock = minItemsInBlock;
    this.maxItemsInBlock = maxItemsInBlock;

    this.maxDoc = state.segmentInfo.maxDoc();
    this.fieldInfos = state.fieldInfos;
    this.postingsWriter = postingsWriter;

    final String termsName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, BlockTreeTermsReader.TERMS_EXTENSION);
    termsOut = state.directory.createOutput(termsName, state.context);
    boolean success = false;
    IndexOutput indexOut = null;
    try {
      CodecUtil.writeIndexHeader(termsOut, BlockTreeTermsReader.TERMS_CODEC_NAME, BlockTreeTermsReader.VERSION_CURRENT,
                                 state.segmentInfo.getId(), state.segmentSuffix);

      final String indexName = IndexFileNames.segmentFileName(state.segmentInfo.name, state.segmentSuffix, BlockTreeTermsReader.TERMS_INDEX_EXTENSION);
      indexOut = state.directory.createOutput(indexName, state.context);
      CodecUtil.writeIndexHeader(indexOut, BlockTreeTermsReader.TERMS_INDEX_CODEC_NAME, BlockTreeTermsReader.VERSION_CURRENT,
                                 state.segmentInfo.getId(), state.segmentSuffix);
      //segment = state.segmentInfo.name;

      postingsWriter.init(termsOut, state);                          // have consumer write its format/header
      
      this.indexOut = indexOut;
      success = true;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(termsOut, indexOut);
      }
    }
  }

  /** Writes the terms file trailer. */
  private void writeTrailer(IndexOutput out, long dirStart) throws IOException {
    out.writeLong(dirStart);    
  }

  /** Writes the index file trailer. */
  private void writeIndexTrailer(IndexOutput indexOut, long dirStart) throws IOException {
    indexOut.writeLong(dirStart);    
  }

  /** Throws {@code IllegalArgumentException} if any of these settings
   *  is invalid. */
  public static void validateSettings(int minItemsInBlock, int maxItemsInBlock) {
    if (minItemsInBlock <= 1) {
      throw new IllegalArgumentException("minItemsInBlock must be >= 2; got " + minItemsInBlock);
    }
    if (minItemsInBlock > maxItemsInBlock) {
      throw new IllegalArgumentException("maxItemsInBlock must be >= minItemsInBlock; got maxItemsInBlock=" + maxItemsInBlock + " minItemsInBlock=" + minItemsInBlock);
    }
    if (2*(minItemsInBlock-1) > maxItemsInBlock) {
      throw new IllegalArgumentException("maxItemsInBlock must be at least 2*(minItemsInBlock-1); got maxItemsInBlock=" + maxItemsInBlock + " minItemsInBlock=" + minItemsInBlock);
    }
  }

  @Override
  public void write(Fields fields) throws IOException {
    //if (DEBUG) System.out.println("\nBTTW.write seg=" + segment);

    String lastField = null;
    for(String field : fields) {
      assert lastField == null || lastField.compareTo(field) < 0;
      lastField = field;

      //if (DEBUG) System.out.println("\nBTTW.write seg=" + segment + " field=" + field);
      Terms terms = fields.terms(field);
      if (terms == null) {
        continue;
      }

      // 通过Terms对象生成TermEnum对象
      TermsEnum termsEnum = terms.iterator();
      TermsWriter termsWriter = new TermsWriter(fieldInfos.fieldInfo(field));
      while (true) {
        // 按照term的字典序依次取出每一个term
        BytesRef term = termsEnum.next();
        //if (DEBUG) System.out.println("BTTW: next term " + term);

        if (term == null) {
          break;
        }

        //if (DEBUG) System.out.println("write field=" + fieldInfo.name + " term=" + brToString(term));
        // 统计包含term的文档号、词频、payload、offset。并生成.doc、.pos、.pay文件
        termsWriter.write(term, termsEnum);
      }

      termsWriter.finish();

      //if (DEBUG) System.out.println("\nBTTW.write done seg=" + segment + " field=" + field);
    }
  }
  
  static long encodeOutput(long fp, boolean hasTerms, boolean isFloor) {
    assert fp < (1L << 62);
    return (fp << 2) | (hasTerms ? BlockTreeTermsReader.OUTPUT_FLAG_HAS_TERMS : 0) | (isFloor ? BlockTreeTermsReader.OUTPUT_FLAG_IS_FLOOR : 0);
  }

  private static class PendingEntry {
    public final boolean isTerm;

    protected PendingEntry(boolean isTerm) {
      this.isTerm = isTerm;
    }
  }

  private static final class PendingTerm extends PendingEntry {
    public final byte[] termBytes;
    // stats + metadata
    public final BlockTermState state;

    public PendingTerm(BytesRef term, BlockTermState state) {
      super(true);
      this.termBytes = new byte[term.length];
      System.arraycopy(term.bytes, term.offset, termBytes, 0, term.length);
      this.state = state;
    }

    @Override
    public String toString() {
      return "TERM: " + brToString(termBytes);
    }
  }

  // for debugging
  @SuppressWarnings("unused")
  static String brToString(BytesRef b) {
    if (b == null) {
      return "(null)";
    } else {
      try {
        return b.utf8ToString() + " " + b;
      } catch (Throwable t) {
        // If BytesRef isn't actually UTF8, or it's eg a
        // prefix of UTF8 that ends mid-unicode-char, we
        // fallback to hex:
        return b.toString();
      }
    }
  }

  // for debugging
  @SuppressWarnings("unused")
  static String brToString(byte[] b) {
    return brToString(new BytesRef(b));
  }

  private static final class PendingBlock extends PendingEntry {
    public final BytesRef prefix;
    // block在.tim文件中的起始位置
    public final long fp;
    public FST<BytesRef> index;
    public List<FST<BytesRef>> subIndices;
    public final boolean hasTerms;
    public final boolean isFloor;
    // 在floor block中第一个entries的字符，比如说处理 "ab"前缀的entries，如果生成了"ab" "abh" "abl"的floor block
    // 那么floorLeadByte值分别对应 "-1" "104" "108",其中104跟108分别是'h' 'l'对应的ASCII码值。
    public final int floorLeadByte;

    public PendingBlock(BytesRef prefix, long fp, boolean hasTerms, boolean isFloor, int floorLeadByte, List<FST<BytesRef>> subIndices) {
      super(false);
      this.prefix = prefix;
      this.fp = fp;
      this.hasTerms = hasTerms;
      this.isFloor = isFloor;
      this.floorLeadByte = floorLeadByte;
      this.subIndices = subIndices;
    }

    @Override
    public String toString() {
      return "BLOCK: prefix=" + brToString(prefix);
    }

    public void compileIndex(List<PendingBlock> blocks, RAMOutputStream scratchBytes, IntsRefBuilder scratchIntsRef) throws IOException {

      assert (isFloor && blocks.size() > 1) || (isFloor == false && blocks.size() == 1): "isFloor=" + isFloor + " blocks=" + blocks;
      assert this == blocks.get(0);

      assert scratchBytes.getFilePointer() == 0;

      // TODO: try writing the leading vLong in MSB order
      // (opposite of what Lucene does today), for better
      // outputs sharing in the FST
      // fp为blocks在.tim文件中的起始位置，hasTerms描述blocks中是否包含pendingTerm对象, isFloor表示是否为floor block
      scratchBytes.writeVLong(encodeOutput(fp, hasTerms, isFloor));
      if (isFloor) {
        // 记录floor block的个数
        scratchBytes.writeVInt(blocks.size()-1);
        for (int i=1;i<blocks.size();i++) {
          PendingBlock sub = blocks.get(i);
          assert sub.floorLeadByte != -1;
          //if (DEBUG) {
          //  System.out.println("    write floorLeadByte=" + Integer.toHexString(sub.floorLeadByte&0xff));
          //}
          scratchBytes.writeByte((byte) sub.floorLeadByte);
          assert sub.fp > fp;
          scratchBytes.writeVLong((sub.fp - fp) << 1 | (sub.hasTerms ? 1 : 0));
        }
      }

      final ByteSequenceOutputs outputs = ByteSequenceOutputs.getSingleton();
      final Builder<BytesRef> indexBuilder = new Builder<>(FST.INPUT_TYPE.BYTE1,
                                                           0, 0, true, false, Integer.MAX_VALUE,
                                                           outputs, true, 15);
      //if (DEBUG) {
      //  System.out.println("  compile index for prefix=" + prefix);
      //}
      //indexBuilder.DEBUG = false;
      final byte[] bytes = new byte[(int) scratchBytes.getFilePointer()];
      assert bytes.length > 0;
      scratchBytes.writeTo(bytes, 0);
      // 往FST中添加一个元素，其中prefix是数据，bytes是prefix附带的值
      indexBuilder.add(Util.toIntsRef(prefix, scratchIntsRef), new BytesRef(bytes, 0, bytes.length));
      scratchBytes.reset();

      // Copy over index for all sub-blocks
      for(PendingBlock block : blocks) {
        if (block.subIndices != null) {
          for(FST<BytesRef> subIndex : block.subIndices) {
            append(indexBuilder, subIndex, scratchIntsRef);
          }
          block.subIndices = null;
        }
      }

      index = indexBuilder.finish();

      assert subIndices == null;

      /*
      Writer w = new OutputStreamWriter(new FileOutputStream("out.dot"));
      Util.toDot(index, w, false, false);
      System.out.println("SAVED to out.dot");
      w.close();
      */
    }

    // TODO: maybe we could add bulk-add method to
    // Builder?  Takes FST and unions it w/ current
    // FST.
    private void append(Builder<BytesRef> builder, FST<BytesRef> subIndex, IntsRefBuilder scratchIntsRef) throws IOException {
      final BytesRefFSTEnum<BytesRef> subIndexEnum = new BytesRefFSTEnum<>(subIndex);
      BytesRefFSTEnum.InputOutput<BytesRef> indexEnt;
      while((indexEnt = subIndexEnum.next()) != null) {
        //if (DEBUG) {
        //  System.out.println("      add sub=" + indexEnt.input + " " + indexEnt.input + " output=" + indexEnt.output);
        //}
        builder.add(Util.toIntsRef(indexEnt.input, scratchIntsRef), indexEnt.output);
      }
    }
  }

  private final RAMOutputStream scratchBytes = new RAMOutputStream();
  private final IntsRefBuilder scratchIntsRef = new IntsRefBuilder();

  static final BytesRef EMPTY_BYTES_REF = new BytesRef();

  class TermsWriter {
    private final FieldInfo fieldInfo;
    private final int longsSize;
    private long numTerms;
    final FixedBitSet docsSeen;
    long sumTotalTermFreq;
    long sumDocFreq;
    long indexStartFP;

    // Records index into pending where the current prefix at that
    // length "started"; for example, if current term starts with 't',
    // startsByPrefix[0] is the index into pending for the first
    // term/sub-block starting with 't'.  We use this to figure out when
    // to write a new block:
    private final BytesRefBuilder lastTerm = new BytesRefBuilder();
    private int[] prefixStarts = new int[8];

    private final long[] longs;

    // Pending stack of terms and blocks.  As terms arrive (in sorted order)
    // we append to this stack, and once the top of the stack has enough
    // terms starting with a common prefix, we write a new block with
    // those terms and replace those terms in the stack with a new block:
    // pending中存放PendingTerm对象。如果有相同前缀的PendingTerm对象超过minItemsInBlock（PendingTerm对象个数可能大于minItemsInBlock）
    // 会将这些PendingTerm对象变成一个PendingBLock。然后用PendingBLock替换掉pending链表中的PendingTerm
    private final List<PendingEntry> pending = new ArrayList<>();

    // Reused in writeBlocks:
    private final List<PendingBlock> newBlocks = new ArrayList<>();

    private PendingTerm firstPendingTerm;
    private PendingTerm lastPendingTerm;

    /** Writes the top count entries in pending, using prevTerm to compute the prefix. */
    void writeBlocks(int prefixLength, int count) throws IOException {

      assert count > 0;

      //if (DEBUG2) {
      //  BytesRef br = new BytesRef(lastTerm.bytes());
      //  br.length = prefixLength;
      //  System.out.println("writeBlocks: seg=" + segment + " prefix=" + brToString(br) + " count=" + count);
      //}

      // Root block better write all remaining pending entries:
      assert prefixLength > 0 || count == pending.size();

      int lastSuffixLeadLabel = -1;

      // True if we saw at least one term in this block (we record if a block
      // only points to sub-blocks in the terms index so we can avoid seeking
      // to it when we are looking for a term):
      boolean hasTerms = false;
      boolean hasSubBlocks = false;

      int start = pending.size()-count;
      int end = pending.size();
      int nextBlockStart = start;
      int nextFloorLeadLabel = -1;

      for (int i=start; i<end; i++) {

        PendingEntry ent = pending.get(i);

        int suffixLeadLabel;

        // ent为PendingTerm对象
        if (ent.isTerm) {
          PendingTerm term = (PendingTerm) ent;
          if (term.termBytes.length == prefixLength) {
            // Suffix is 0, i.e. prefix 'foo' and term is
            // 'foo' so the term has empty string suffix
            // in this block
            assert lastSuffixLeadLabel == -1: "i=" + i + " lastSuffixLeadLabel=" + lastSuffixLeadLabel;
            suffixLeadLabel = -1;
          } else {
            // 取相同前缀的后一个字节，例子：如果当前term是 "abe", prefixLength的值为2，那么suffixLeadLabel的值为 “e”
            suffixLeadLabel = term.termBytes[prefixLength] & 0xff;
          }
        } else {
          // ent为PendingBlock对象
          PendingBlock block = (PendingBlock) ent;
          assert block.prefix.length > prefixLength;
          suffixLeadLabel = block.prefix.bytes[block.prefix.offset + prefixLength] & 0xff;
        }
        // if (DEBUG) System.out.println("  i=" + i + " ent=" + ent + " suffixLeadLabel=" + suffixLeadLabel);

        if (suffixLeadLabel != lastSuffixLeadLabel) {
          int itemsInBlock = i - nextBlockStart;
          // 一个Block中iterm的个数是 25~48 (minItemsInBlock ~ maxItemsInBlock), 如果超过maxItemsInBlock，那么先将部分iterm生成一个floor block
          if (itemsInBlock >= minItemsInBlock && end-nextBlockStart > maxItemsInBlock) {
            // The count is too large for one block, so we must break it into "floor" blocks, where we record
            // the leading label of the suffix of the first term in each floor block, so at search time we can
            // jump to the right floor block.  We just use a naive greedy segmenter here: make a new floor
            // block as soon as we have at least minItemsInBlock.  This is not always best: it often produces
            // a too-small block as the final block:
            // 每一个floor block的leading label是floor block中第一个term的后缀，这样的话在搜索阶段就能跳转到正确的floor block。
            boolean isFloor = itemsInBlock < count;
            // nextBlockStart跟i跟别描述了在pending数组中需要处理成一个floor block的范围
            newBlocks.add(writeBlock(prefixLength, isFloor, nextFloorLeadLabel, nextBlockStart, i, hasTerms, hasSubBlocks));

            hasTerms = false;
            hasSubBlocks = false;
            // floor block中第一个entries的字符，比如说处理 "ab"前缀的entries，如果生成了"ab" "abh" "abl"
            // 那么nextFloorLeadLabel的值分别对应 "-1" "104" "108",其中104跟108分别是'h' 'l'对应的ASCII码值。
            nextFloorLeadLabel = suffixLeadLabel;
            nextBlockStart = i;
          }

          lastSuffixLeadLabel = suffixLeadLabel;
        }

        if (ent.isTerm) {
          hasTerms = true;
        } else {
          hasSubBlocks = true;
        }
      }

      // Write last block, if any:
      if (nextBlockStart < end) {
        int itemsInBlock = end - nextBlockStart;
        boolean isFloor = itemsInBlock < count;
        newBlocks.add(writeBlock(prefixLength, isFloor, nextFloorLeadLabel, nextBlockStart, end, hasTerms, hasSubBlocks));
      }

      assert newBlocks.isEmpty() == false;

      PendingBlock firstBlock = newBlocks.get(0);

      assert firstBlock.isFloor || newBlocks.size() == 1;

      firstBlock.compileIndex(newBlocks, scratchBytes, scratchIntsRef);

      // Remove slice from the top of the pending stack, that we just wrote:
      pending.subList(pending.size()-count, pending.size()).clear();

      // Append new block
      pending.add(firstBlock);

      newBlocks.clear();
    }

    /** Writes the specified slice (start is inclusive, end is exclusive)
     *  from pending stack as a new block.  If isFloor is true, there
     *  were too many (more than maxItemsInBlock) entries sharing the
     *  same prefix, and so we broke it into multiple floor blocks where
     *  we record the starting label of the suffix of each floor block. */
    // 如果相同前缀的entries的个数超过maxItemInBlock，那么会分成多个floor blocks, 并且每个floor block的floorLeadLabel为相同后缀的下一个字节
    //比如说处理包含相同前缀 "ab"的term，如果生成了"ab" "abh" "abl"的floor blocks，那么floorLeadLabel的值分别是"-1" "104" "108",其中104跟108分别是'h' 'l'对应的ASCII码值。
    private PendingBlock writeBlock(int prefixLength, boolean isFloor, int floorLeadLabel, int start, int end,
                                    boolean hasTerms, boolean hasSubBlocks) throws IOException {

      assert end > start;

      // .tim文件中下一个可以写入数据的位置
      long startFP = termsOut.getFilePointer();

      // floorLeadLabel为true，说明相同前缀的term的个数需要至少一个block来存储，一个block中最少包含25个、最多包含45个term的信息
      boolean hasFloorLeadLabel = isFloor && floorLeadLabel != -1;

      final BytesRef prefix = new BytesRef(prefixLength + (hasFloorLeadLabel ? 1 : 0));
      System.arraycopy(lastTerm.get().bytes, 0, prefix.bytes, 0, prefixLength);
      prefix.length = prefixLength;

      //if (DEBUG2) System.out.println("    writeBlock field=" + fieldInfo.name + " prefix=" + brToString(prefix) + " fp=" + startFP + " isFloor=" + isFloor + " isLastInFloor=" + (end == pending.size()) + " floorLeadLabel=" + floorLeadLabel + " start=" + start + " end=" + end + " hasTerms=" + hasTerms + " hasSubBlocks=" + hasSubBlocks);

      // Write block header:
      // numEntries的个数为当前block准备处理的term个数
      int numEntries = end - start;
      int code = numEntries << 1;
      // if语句为真，说明这是最后一个block，这个block可能是NodeBlock最后一个block
      if (end == pending.size()) {
        // Last block:
        code |= 1;
      }
      termsOut.writeVInt(code);

      /*
      if (DEBUG) {
        System.out.println("  writeBlock " + (isFloor ? "(floor) " : "") + "seg=" + segment + " pending.size()=" + pending.size() + " prefixLength=" + prefixLength + " indexPrefix=" + brToString(prefix) + " entCount=" + (end-start+1) + " startFP=" + startFP + (isFloor ? (" floorLeadLabel=" + Integer.toHexString(floorLeadLabel)) : ""));
      }
      */

      // 1st pass: pack term suffix bytes into byte[] blob
      // TODO: cutover to bulk int codec... simple64?

      // We optimize the leaf block case (block has only terms), writing a more
      // compact format in this case:

      // 判断当前处理的pendingEntry中包含了pendingBlock。
      boolean isLeafBlock = hasSubBlocks == false;

      //System.out.println("  isLeaf=" + isLeafBlock);

      final List<FST<BytesRef>> subIndices;

      boolean absolute = true;

      // 只有pendingTerm
      if (isLeafBlock) {
        // Block contains only ordinary terms:
        subIndices = null;
        for (int i=start;i<end;i++) {
          PendingEntry ent = pending.get(i);
          assert ent.isTerm: "i=" + i;
          // ent肯定是PendingTerm类型，所以这里直接强制转化
          PendingTerm term = (PendingTerm) ent;

          assert StringHelper.startsWith(term.termBytes, prefix): "term.term=" + term.termBytes + " prefix=" + prefix;
          BlockTermState state = term.state;
          // 取出当前term的相同的后缀的长度
          final int suffix = term.termBytes.length - prefixLength;
          //if (DEBUG2) {
          //  BytesRef suffixBytes = new BytesRef(suffix);
          //  System.arraycopy(term.termBytes, prefixLength, suffixBytes.bytes, 0, suffix);
          //  suffixBytes.length = suffix;
          //  System.out.println("    write term suffix=" + brToString(suffixBytes));
          //}

          // For leaf block we write suffix straight
          // 写入后缀长度个数
          suffixWriter.writeVInt(suffix);
          // 写入前缀值
          suffixWriter.writeBytes(term.termBytes, prefixLength, suffix);
          assert floorLeadLabel == -1 || (term.termBytes[prefixLength] & 0xff) >= floorLeadLabel;

          // Write term stats, to separate byte[] blob:
          // 写入包含当前term的文档个数
          statsWriter.writeVInt(state.docFreq);
          if (fieldInfo.getIndexOptions() != IndexOptions.DOCS) {
            assert state.totalTermFreq >= state.docFreq: state.totalTermFreq + " vs " + state.docFreq;
            // totalTermFreq是所有文档中term的出现的总数（非去重）, 这里目的是存放totalTermFreq，不过用了差值存储(真的是尽力压缩数据呀)
            statsWriter.writeVLong(state.totalTermFreq - state.docFreq);
          }

          // Write term meta data
          postingsWriter.encodeTerm(longs, bytesWriter, fieldInfo, state, absolute);
          for (int pos = 0; pos < longsSize; pos++) {
            assert longs[pos] >= 0;
            // 写入 term在 .doc, .pos ，payload文件中的位置信息(可能是差值)
            metaWriter.writeVLong(longs[pos]);
          }
          // 将bytesWriter中的数据合并到metaWriter中
          bytesWriter.writeTo(metaWriter);
          bytesWriter.reset();
          absolute = false;
        }
      } else {
        // Block has at least one prefix term or a sub block:
        subIndices = new ArrayList<>();
        for (int i=start;i<end;i++) {
          PendingEntry ent = pending.get(i);
          // 处理pendingTerm
          if (ent.isTerm) {
            PendingTerm term = (PendingTerm) ent;

            assert StringHelper.startsWith(term.termBytes, prefix): "term.term=" + term.termBytes + " prefix=" + prefix;
            BlockTermState state = term.state;
            final int suffix = term.termBytes.length - prefixLength;
            //if (DEBUG2) {
            //  BytesRef suffixBytes = new BytesRef(suffix);
            //  System.arraycopy(term.termBytes, prefixLength, suffixBytes.bytes, 0, suffix);
            //  suffixBytes.length = suffix;
            //  System.out.println("      write term suffix=" + brToString(suffixBytes));
            //}

            // For non-leaf block we borrow 1 bit to record
            // if entry is term or sub-block, and 1 bit to record if
            // it's a prefix term.  Terms cannot be larger than ~32 KB
            // so we won't run out of bits:

            suffixWriter.writeVInt(suffix << 1);
            suffixWriter.writeBytes(term.termBytes, prefixLength, suffix);

            // Write term stats, to separate byte[] blob:
            statsWriter.writeVInt(state.docFreq);
            if (fieldInfo.getIndexOptions() != IndexOptions.DOCS) {
              assert state.totalTermFreq >= state.docFreq;
              statsWriter.writeVLong(state.totalTermFreq - state.docFreq);
            }

            // TODO: now that terms dict "sees" these longs,
            // we can explore better column-stride encodings
            // to encode all long[0]s for this block at
            // once, all long[1]s, etc., e.g. using
            // Simple64.  Alternatively, we could interleave
            // stats + meta ... no reason to have them
            // separate anymore:

            // Write term meta data
            postingsWriter.encodeTerm(longs, bytesWriter, fieldInfo, state, absolute);
            for (int pos = 0; pos < longsSize; pos++) {
              assert longs[pos] >= 0;
              metaWriter.writeVLong(longs[pos]);
            }
            bytesWriter.writeTo(metaWriter);
            bytesWriter.reset();
            absolute = false;
            // 处理pendingBlock
          } else {
            PendingBlock block = (PendingBlock) ent;
            assert StringHelper.startsWith(block.prefix, prefix);
            final int suffix = block.prefix.length - prefixLength;
            assert StringHelper.startsWith(block.prefix, prefix);

            assert suffix > 0;

            // For non-leaf block we borrow 1 bit to record
            // if entry is term or sub-block:f
            suffixWriter.writeVInt((suffix<<1)|1);
            suffixWriter.writeBytes(block.prefix.bytes, prefixLength, suffix);

            //if (DEBUG2) {
            //  BytesRef suffixBytes = new BytesRef(suffix);
            //  System.arraycopy(block.prefix.bytes, prefixLength, suffixBytes.bytes, 0, suffix);
            //  suffixBytes.length = suffix;
            //  System.out.println("      write sub-block suffix=" + brToString(suffixBytes) + " subFP=" + block.fp + " subCode=" + (startFP-block.fp) + " floor=" + block.isFloor);
            //}

            assert floorLeadLabel == -1 || (block.prefix.bytes[prefixLength] & 0xff) >= floorLeadLabel: "floorLeadLabel=" + floorLeadLabel + " suffixLead=" + (block.prefix.bytes[prefixLength] & 0xff);
            assert block.fp < startFP;

            suffixWriter.writeVLong(startFP - block.fp);
            subIndices.add(block.index);
          }
        }

        assert subIndices.size() != 0;
      }

      // TODO: we could block-write the term suffix pointers;
      // this would take more space but would enable binary
      // search on lookup

      // 将临时的suffixWriter、statsWriter、metaWriter的数据合并到.tim文件中
      // Write suffixes byte[] blob to terms dict output:
      // 将所有的term的后缀信息合并到.tim文件中
      termsOut.writeVInt((int) (suffixWriter.getFilePointer() << 1) | (isLeafBlock ? 1:0));
      suffixWriter.writeTo(termsOut);
      suffixWriter.reset();

      // Write term stats byte[] blob
      // 将所有的term的 termStats信息合并到.tim文件中，每个term的termStats描述是 包含term的词频，term在所有文档中的出现的次数
      termsOut.writeVInt((int) statsWriter.getFilePointer());
      statsWriter.writeTo(termsOut);
      statsWriter.reset();

      // Write term meta data byte[] blob
      // 将所有的term的 metadata信息合并到.tim文件中，每个term的metadata描述的是在.doc、.pay、.pos文件中的一些信息。
      termsOut.writeVInt((int) metaWriter.getFilePointer());
      metaWriter.writeTo(termsOut);
      metaWriter.reset();

      // 看到这里才明白为什么要使用3个临时的IndexOutput对象，呵呵。

      // if (DEBUG) {
      //   System.out.println("      fpEnd=" + out.getFilePointer());
      // }

      if (hasFloorLeadLabel) {
        // We already allocated to length+1 above:
        prefix.bytes[prefix.length++] = (byte) floorLeadLabel;
      }
      return new PendingBlock(prefix, startFP, hasTerms, isFloor, floorLeadLabel, subIndices);
    }

    TermsWriter(FieldInfo fieldInfo) {
      this.fieldInfo = fieldInfo;
      assert fieldInfo.getIndexOptions() != IndexOptions.NONE;
      docsSeen = new FixedBitSet(maxDoc);

      this.longsSize = postingsWriter.setField(fieldInfo);
      this.longs = new long[longsSize];
    }
    
    /** Writes one term's worth of postings. */
    public void write(BytesRef text, TermsEnum termsEnum) throws IOException {
      /*
      if (DEBUG) {
        int[] tmp = new int[lastTerm.length];
        System.arraycopy(prefixStarts, 0, tmp, 0, tmp.length);
        System.out.println("BTTW: write term=" + brToString(text) + " prefixStarts=" + Arrays.toString(tmp) + " pending.size()=" + pending.size());
      }
      */

      // 从倒排表中取出term的所有信息
      BlockTermState state = postingsWriter.writeTerm(text, termsEnum, docsSeen);
      if (state != null) {

        assert state.docFreq != 0;
        assert fieldInfo.getIndexOptions() == IndexOptions.DOCS || state.totalTermFreq >= state.docFreq: "postingsWriter=" + postingsWriter;
        pushTerm(text);

        // 每一个term生成一个PendingTerm
        PendingTerm term = new PendingTerm(text, state);
        pending.add(term);
        //if (DEBUG) System.out.println("    add pending term = " + text + " pending.size()=" + pending.size());

        // 包含当前term的文档数
        sumDocFreq += state.docFreq;
        // term在所有文档后中的总数
        sumTotalTermFreq += state.totalTermFreq;
        // 域中的term个数(去重)
        numTerms++;
        if (firstPendingTerm == null) {
          firstPendingTerm = term;
        }
        lastPendingTerm = term;
      }
    }

    /** Pushes the new term to the top of the stack, and writes new blocks. */
    private void pushTerm(BytesRef text) throws IOException {
      int limit = Math.min(lastTerm.length(), text.length);

      // Find common prefix between last term and current term:
      int pos = 0;
      // 统计当前term跟上一个term相同前缀的个数
      while (pos < limit && lastTerm.byteAt(pos) == text.bytes[text.offset+pos]) {
        pos++;
      }

      // if (DEBUG) System.out.println("  shared=" + pos + "  lastTerm.length=" + lastTerm.length);

      // Close the "abandoned" suffix now:
      for(int i=lastTerm.length()-1;i>=pos;i--) {
        // How many items on top of the stack share the current suffix
        // we are closing:
        // 这里获取具有相同前缀的term的个数最多的
        int prefixTopSize = pending.size() - prefixStarts[i];
        // 如果相同前缀的term的个数超过25（默认minItemsInBlock的大小为25），那么生成一个PendingBlock对象
        if (prefixTopSize >= minItemsInBlock) {
          // if (DEBUG) System.out.println("pushTerm i=" + i + " prefixTopSize=" + prefixTopSize + " minItemsInBlock=" + minItemsInBlock);
          writeBlocks(i+1, prefixTopSize);
          prefixStarts[i] -= prefixTopSize-1;
        }
      }

      // 数组动态扩展
      if (prefixStarts.length < text.length) {
        prefixStarts = ArrayUtil.grow(prefixStarts, text.length);
      }

      // Init new tail:
      for(int i=pos;i<text.length;i++) {
        prefixStarts[i] = pending.size();
      }

      lastTerm.copyBytes(text);
    }

    // Finishes all terms in this field
    // 结束当前域的所有term的处理
    public void finish() throws IOException {
      if (numTerms > 0) {
        // if (DEBUG) System.out.println("BTTW: finish prefixStarts=" + Arrays.toString(prefixStarts));

        // Add empty term to force closing of all final blocks:
        pushTerm(new BytesRef());

        // TODO: if pending.size() is already 1 with a non-zero prefix length
        // we can save writing a "degenerate" root block, but we have to
        // fix all the places that assume the root block's prefix is the empty string:
        pushTerm(new BytesRef());
        // pending中包含了pendingTerm跟pendingBlock对象。这里是最后一步将这些PendingEntry对象归并为block
        writeBlocks(0, pending.size());

        // We better have one final "root" block:
        assert pending.size() == 1 && !pending.get(0).isTerm: "pending.size()=" + pending.size() + " pending=" + pending;
        final PendingBlock root = (PendingBlock) pending.get(0);
        assert root.prefix.length == 0;
        assert root.index.getEmptyOutput() != null;

        // Write FST to index
        indexStartFP = indexOut.getFilePointer();
        root.index.save(indexOut);
        //System.out.println("  write FST " + indexStartFP + " field=" + fieldInfo.name);

        /*
        if (DEBUG) {
          final String dotFileName = segment + "_" + fieldInfo.name + ".dot";
          Writer w = new OutputStreamWriter(new FileOutputStream(dotFileName));
          Util.toDot(root.index, w, false, false);
          System.out.println("SAVED to " + dotFileName);
          w.close();
        }
        */
        assert firstPendingTerm != null;
        // 最小的那个term
        BytesRef minTerm = new BytesRef(firstPendingTerm.termBytes);

        assert lastPendingTerm != null;
        // 最大的那个term
        BytesRef maxTerm = new BytesRef(lastPendingTerm.termBytes);

        fields.add(new FieldMetaData(fieldInfo,
                                     ((PendingBlock) pending.get(0)).index.getEmptyOutput(),
                                     numTerms,
                                     indexStartFP,
                                     sumTotalTermFreq,
                                     sumDocFreq,
                                     docsSeen.cardinality(),
                                     longsSize,
                                     minTerm, maxTerm));
      } else {
        assert sumTotalTermFreq == 0 || fieldInfo.getIndexOptions() == IndexOptions.DOCS && sumTotalTermFreq == -1;
        assert sumDocFreq == 0;
        assert docsSeen.cardinality() == 0;
      }
    }

    // 临时用来记录term前缀信息
    private final RAMOutputStream suffixWriter = new RAMOutputStream();
    // 临时用来记录term文档号词频信息
    private final RAMOutputStream statsWriter = new RAMOutputStream();
    // 临时用来记录term的某个term在.doc、.pos payload文件中的位置信息（差值）
    private final RAMOutputStream metaWriter = new RAMOutputStream();
    private final RAMOutputStream bytesWriter = new RAMOutputStream();
  }

  private boolean closed;
  
  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    
    boolean success = false;
    try {
      
      final long dirStart = termsOut.getFilePointer();
      final long indexDirStart = indexOut.getFilePointer();

      termsOut.writeVInt(fields.size());
      
      for(FieldMetaData field : fields) {
        //System.out.println("  field " + field.fieldInfo.name + " " + field.numTerms + " terms");
        // 当前域的id
        termsOut.writeVInt(field.fieldInfo.number);
        assert field.numTerms > 0;
        // 当前域中的term的种类
        termsOut.writeVLong(field.numTerms);
        termsOut.writeVInt(field.rootCode.length);
        termsOut.writeBytes(field.rootCode.bytes, field.rootCode.offset, field.rootCode.length);
        assert field.fieldInfo.getIndexOptions() != IndexOptions.NONE;
        if (field.fieldInfo.getIndexOptions() != IndexOptions.DOCS) {
          termsOut.writeVLong(field.sumTotalTermFreq);
        }
        termsOut.writeVLong(field.sumDocFreq);
        termsOut.writeVInt(field.docCount);
        termsOut.writeVInt(field.longsSize);
        indexOut.writeVLong(field.indexStartFP);
        writeBytesRef(termsOut, field.minTerm);
        writeBytesRef(termsOut, field.maxTerm);
      }
      writeTrailer(termsOut, dirStart);
      CodecUtil.writeFooter(termsOut);
      writeIndexTrailer(indexOut, indexDirStart);
      CodecUtil.writeFooter(indexOut);
      success = true;
    } finally {
      if (success) {
        IOUtils.close(termsOut, indexOut, postingsWriter);
      } else {
        IOUtils.closeWhileHandlingException(termsOut, indexOut, postingsWriter);
      }
    }
  }

  private static void writeBytesRef(IndexOutput out, BytesRef bytes) throws IOException {
    out.writeVInt(bytes.length);
    out.writeBytes(bytes.bytes, bytes.offset, bytes.length);
  }
}
