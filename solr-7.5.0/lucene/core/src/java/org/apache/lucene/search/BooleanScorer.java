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
package org.apache.lucene.search;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FutureObjects;
import org.apache.lucene.util.PriorityQueue;

/**
 * {@link BulkScorer} that is used for pure disjunctions and disjunctions
 * that have low values of {@link BooleanQuery.Builder#setMinimumNumberShouldMatch(int)}
 * and dense clauses. This scorer scores documents by batches of 2048 docs.
 */
final class BooleanScorer extends BulkScorer {

  static final int SHIFT = 11;
  static final int SIZE = 1 << SHIFT;
  static final int MASK = SIZE - 1;
  static final int SET_SIZE = 1 << (SHIFT - 6);
  static final int SET_MASK = SET_SIZE - 1;

  static class Bucket {
    // 某个文档的打分
    double score;
    // 这个文档号出现的次数，用来跟minShouldMatch比较，判断是否满足要求
    int freq;
  }

  private class BulkScorerAndDoc {
    final BulkScorer scorer;
    // 包含这个域值的文档个数(去重)
    final long cost;
    // 初始值为-1，用来表示包含这个域值的正在处理处理的文档号(从最小的文档号开始)
    int next;

    BulkScorerAndDoc(BulkScorer scorer) {
        // 存放某个域值对应的BulkScorer对象
      this.scorer = scorer;
      // 包含这个域值的文档个数
      this.cost = scorer.cost();
      this.next = -1;
    }

    // 找到下一个不小于min的文档号
    void advance(int min) throws IOException {
      score(orCollector, null, min, min);
    }

    void score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
      next = scorer.score(collector, acceptDocs, min, max);
    }
  }

  // See MinShouldMatchSumScorer for an explanation
  private static long cost(Collection<BulkScorer> scorers, int minShouldMatch) {
    final PriorityQueue<BulkScorer> pq = new PriorityQueue<BulkScorer>(scorers.size() - minShouldMatch + 1) {
      @Override
      protected boolean lessThan(BulkScorer a, BulkScorer b) {
        return a.cost() > b.cost();
      }
    };
    for (BulkScorer scorer : scorers) {
      pq.insertWithOverflow(scorer);
    }
    long cost = 0;
    for (BulkScorer scorer = pq.pop(); scorer != null; scorer = pq.pop()) {
      cost += scorer.cost();
    }
    return cost;
  }

  static final class HeadPriorityQueue extends PriorityQueue<BulkScorerAndDoc> {

    public HeadPriorityQueue(int maxSize) {
      super(maxSize);
    }

    @Override
    protected boolean lessThan(BulkScorerAndDoc a, BulkScorerAndDoc b) {
      // 比较规则：next是值要处理的文档号，越小则排在前面
      return a.next < b.next;
    }

  }

  static final class TailPriorityQueue extends PriorityQueue<BulkScorerAndDoc> {

    public TailPriorityQueue(int maxSize) {
      super(maxSize);
    }

    @Override
    protected boolean lessThan(BulkScorerAndDoc a, BulkScorerAndDoc b) {
        // 比较规则：比较包含某个域值的文档个数
      return a.cost < b.cost;
    }

    public BulkScorerAndDoc get(int i) {
      FutureObjects.checkIndex(i, size());
      return (BulkScorerAndDoc) getHeapArray()[1 + i];
    }

  }

  // 数组下标是文档号(经过MASK)，数组元素记录了这个文档号出现的次数跟分数
  final Bucket[] buckets = new Bucket[SIZE];
  // This is basically an inlined FixedBitSet... seems to help with bound checks
  // 用来去重的存储文档号, 二进制表示的数值中，每个为1的bit位的所属第几位就是文档号的值
  // 比如 01001, 说明存储了 文档号 0跟3
  final long[] matching = new long[SET_SIZE];

  final BulkScorerAndDoc[] leads;
  // 存放那些包含某个域值的文档个数相对较少的BulkScorerAndDoc对象
  final HeadPriorityQueue head;
  // 存放那些包含某个域值的文档个数相对较多的BulkScorerAndDoc对象
  final TailPriorityQueue tail;
  final FakeScorer fakeScorer = new FakeScorer();
  final int minShouldMatch;
  final long cost;

  final class OrCollector implements LeafCollector {
    Scorer scorer;

    @Override
    public void setScorer(Scorer scorer) {
      this.scorer = scorer;
    }

    @Override
    public void collect(int doc) throws IOException {
      final int i = doc & MASK;
      final int idx = i >>> 6;
      // 用来去重的存储文档号, 二进制表示的数值中，每个为1的bit位的所属第几位就是文档号的值
      // 比如 01001, 说明存储了 文档号 0跟3
      matching[idx] |= 1L << i;
      // 引用bucket对象，buckets[]数组下标是文档号
      // bucket中的freq统计次文档号出现的次数
      final Bucket bucket = buckets[i];
      bucket.freq++;
      // 这里可以看出，打分是一个累加的过程
      bucket.score += scorer.score();
    }
  }

  // 这个Collector的collect(int)方法用来处理 this.buckets数组，this.matching数组的值
  final OrCollector orCollector = new OrCollector();

  BooleanScorer(BooleanWeight weight, Collection<BulkScorer> scorers, int minShouldMatch, boolean needsScores) {
    if (minShouldMatch < 1 || minShouldMatch > scorers.size()) {
      throw new IllegalArgumentException("minShouldMatch should be within 1..num_scorers. Got " + minShouldMatch);
    }
    if (scorers.size() <= 1) {
      throw new IllegalArgumentException("This scorer can only be used with two scorers or more, got " + scorers.size());
    }
    // 初始化this.buckets数组，数组的下标表示文档号，数组元素则是Bucket对象，用来记录某个文档号出现的次数跟打分
    for (int i = 0; i < buckets.length; i++) {
      buckets[i] = new Bucket();
    }
    this.leads = new BulkScorerAndDoc[scorers.size()];
    this.head = new HeadPriorityQueue(scorers.size() - minShouldMatch + 1);
    this.tail = new TailPriorityQueue(minShouldMatch - 1);
    this.minShouldMatch = minShouldMatch;
    for (BulkScorer scorer : scorers) {
      if (needsScores == false) {
        // OrCollector calls score() all the time so we have to explicitly
        // disable scoring in order to avoid decoding useless norms
        scorer = BooleanWeight.disableScoring(scorer);
      }
      // 插入一个新的对象到一个优先级队列中，当队列已满的时候返回cost最小的对象, 队列不满的话返回null
      final BulkScorerAndDoc evicted = tail.insertWithOverflow(new BulkScorerAndDoc(scorer));
      if (evicted != null) {
        head.add(evicted);
      }
    }
    this.cost = cost(scorers, minShouldMatch);
  }

  @Override
  public long cost() {
    return cost;
  }

  // 在这个方法中，才真正的把满足要求的文档号传给collector, 这个Collector会在展示结果时使用到
  private void scoreDocument(LeafCollector collector, int base, int i) throws IOException {
    final FakeScorer fakeScorer = this.fakeScorer;
    final Bucket bucket = buckets[i];
    // if语句为true：文档号出现的次数满足minShouldMatch
    if (bucket.freq >= minShouldMatch) {
      fakeScorer.score = (float) bucket.score;
      // 根据之前得到的windowBase值，恢复文档号真正的值
      final int doc = base | i;
      fakeScorer.doc = doc;
      collector.collect(doc);
    }
    bucket.freq = 0;
    bucket.score = 0;
  }

  private void scoreMatches(LeafCollector collector, int base) throws IOException {
    long matching[] = this.matching;
    for (int idx = 0; idx < matching.length; idx++) {
      long bits = matching[idx];
      // 反序列化的过程，得到所有的文档号
      while (bits != 0L) {
        int ntz = Long.numberOfTrailingZeros(bits);
        int doc = idx << 6 | ntz;
        scoreDocument(collector, base, doc);
        bits ^= 1L << ntz;
      }
    }
  }

  private void scoreWindowIntoBitSetAndReplay(LeafCollector collector, Bits acceptDocs,
      int base, int min, int max, BulkScorerAndDoc[] scorers, int numScorers) throws IOException {
      // 遍历每一个BulkScorerAndDoc，统计文档号出现的次数跟打分
    for (int i = 0; i < numScorers; ++i) {
      final BulkScorerAndDoc scorer = scorers[i];
      assert scorer.next < max;
      // 这里传入this.orCollector，通过这个对象来统计文档号出现的次数(次数大于minShouldMatch的话，说明这个文档号满足搜索要求)跟打分
      scorer.score(orCollector, acceptDocs, min, max);
    }

    scoreMatches(collector, base);
    Arrays.fill(matching, 0L);
  }

  private BulkScorerAndDoc advance(int min) throws IOException {
    assert tail.size() == minShouldMatch - 1;
    final HeadPriorityQueue head = this.head;
    final TailPriorityQueue tail = this.tail;
    BulkScorerAndDoc headTop = head.top();
    BulkScorerAndDoc tailTop = tail.top();
    // min值指的是文档号，我们要处理从min开始的文档号
    // 所以遍历每一个优先级队列head中的对象，通过查询倒排表来找到下一个不小于min的文档号
    // 每次更新一个head钟的对象后就进行调整堆，当headTop.next都不小于min，说明优先级队列中所有的对象的next都更新到了不小于min的值（查看lessThan的比较规则）
    while (headTop.next < min) {
      if (tailTop == null || headTop.cost <= tailTop.cost) {
        // 查询倒排表，或者下一个文档号
        headTop.advance(min);
        // 调整堆（最小堆）
        headTop = head.updateTop();
      } else {
        // swap the top of head and tail
        final BulkScorerAndDoc previousHeadTop = headTop;
        tailTop.advance(min);
        headTop = head.updateTop(tailTop);
        tailTop = tail.updateTop(previousHeadTop);
      }
    }
    return headTop;
  }

  private void scoreWindowMultipleScorers(LeafCollector collector, Bits acceptDocs, int windowBase, int windowMin, int windowMax, int maxFreq) throws IOException {
      // if语句为true: 说明优先级队列this.head中的个数(优先从这些对象所属的文档考虑,原因看this.head的注释)达不到minShouldMatch
    // 那么我们需要从优先级队列this.tail中挑出一些候选者
      // 这种情况发生在minShould的值大于BooleanQuery中的clause的一半
    while (maxFreq < minShouldMatch && maxFreq + tail.size() >= minShouldMatch) {
      // a match is still possible
      final BulkScorerAndDoc candidate = tail.pop();
      candidate.advance(windowMin);
      if (candidate.next < windowMax) {
        leads[maxFreq++] = candidate;
      } else {
        // 如果next值大于windowMax，那么放到this.head, 等待下次再处理
        head.add(candidate);
      }
    }

    // 尽管this.tail中的对象中包含的某些文档相对于this.head中的包含的文档满足minShouldMatch的可能性低点，但还是会进行一个考察
    // 所以在lead[]数组中，下标越小的BulkScorerAndDoc对象中包含的文档满足搜索条件的可能性越高
    if (maxFreq >= minShouldMatch) {
      // There might be matches in other scorers from the tail too
      for (int i = 0; i < tail.size(); ++i) {
        leads[maxFreq++] = tail.get(i);
      }
      tail.clear();

      scoreWindowIntoBitSetAndReplay(collector, acceptDocs, windowBase, windowMin, windowMax, leads, maxFreq);
    }

    // Push back scorers into head and tail
    for (int i = 0; i < maxFreq; ++i) {
      final BulkScorerAndDoc evicted = head.insertWithOverflow(leads[i]);
      if (evicted != null) {
        tail.add(evicted);
      }
    }
  }

  private void scoreWindowSingleScorer(BulkScorerAndDoc bulkScorer, LeafCollector collector,
      Bits acceptDocs, int windowMin, int windowMax, int max) throws IOException {
    assert tail.size() == 0;
    final int nextWindowBase = head.top().next & ~MASK;
    final int end = Math.max(windowMax, Math.min(max, nextWindowBase));

    bulkScorer.score(collector, acceptDocs, windowMin, end);

    // reset the scorer that should be used for the general case
    collector.setScorer(fakeScorer);
  }

  private BulkScorerAndDoc scoreWindow(BulkScorerAndDoc top, LeafCollector collector,
      Bits acceptDocs, int min, int max) throws IOException {
      // 把文档号变成0~2047返回的值，，这里求出windowBase用来在最后的时候恢复文档号的原值
    final int windowBase = top.next & ~MASK; // find the window that the next match belongs to
    final int windowMin = Math.max(min, windowBase);
    final int windowMax = Math.min(max, windowBase + SIZE);

    // Fill 'leads' with all scorers from 'head' that are in the right window
      // next值最小的BulkScorerAndDoc对象放到leads[]数组中的第一个第一个位置
    leads[0] = head.pop();
    int maxFreq = 1;
    // 取出优先级队列this.head中的所有BulkScorerAndDoc对象，放到leads[]数组中
    while (head.size() > 0 && head.top().next < windowMax) {
      leads[maxFreq++] = head.pop();
    }

    if (minShouldMatch == 1 && maxFreq == 1) {
      // special case: only one scorer can match in the current window,
      // we can collect directly
      final BulkScorerAndDoc bulkScorer = leads[0];
      scoreWindowSingleScorer(bulkScorer, collector, acceptDocs, windowMin, windowMax, max);
      return head.add(bulkScorer);
    } else {
      // general case, collect through a bit set first and then replay
      scoreWindowMultipleScorers(collector, acceptDocs, windowBase, windowMin, windowMax, maxFreq);
      return head.top();
    }
  }

  @Override
  // 调用此方法后, 我们就能得到搜索的结果，而这些结果其实就是docId，然后将docId传给Collector完成结果的收集工作
  public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
    fakeScorer.doc = -1;
    collector.setScorer(fakeScorer);

    // 获得next值最小的那个BulkScorerAndDoc对象
    BulkScorerAndDoc top = advance(min);
    while (top.next < max) {
      top = scoreWindow(top, collector, acceptDocs, min, max);
    }

    return top.next;
  }

}
