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
package org.apache.lucene.search.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

/** FirstPassGroupingCollector is the first of two passes necessary
 *  to collect grouped hits.  This pass gathers the top N sorted
 *  groups. Groups are defined by a {@link GroupSelector}
 *
 *  <p>See {@link org.apache.lucene.search.grouping} for more
 *  details including a full code example.</p>
 *
 * @lucene.experimental
 */
public class FirstPassGroupingCollector<T> extends SimpleCollector {

  private final GroupSelector<T> groupSelector;

  private final FieldComparator<?>[] comparators;
  private final LeafFieldComparator[] leafComparators;
  // 每一个数组元素对应排序规则之一是否为倒序
  private final int[] reversed;
  private final int topNGroups;
  private final boolean needsScores;
  private final HashMap<T, CollectedSearchGroup<T>> groupMap;
  // 遍历所有comparators时候需要用到的上限值, 用来控制遍历的次数(规则个数)
  private final int compIDXEnd;

  // Set once we reach topNGroups unique groups:
  /** @lucene.internal */
  // 排序后的group
  protected TreeSet<CollectedSearchGroup<T>> orderedGroups;
  private int docBase;
  private int spareSlot;

  /**
   * Create the first pass collector.
   *
   * @param groupSelector a GroupSelector used to defined groups
   * @param groupSort The {@link Sort} used to sort the
   *    groups.  The top sorted document within each group
   *    according to groupSort, determines how that group
   *    sorts against other groups.  This must be non-null,
   *    ie, if you want to groupSort by relevance use
   *    Sort.RELEVANCE.
   * @param topNGroups How many top groups to keep.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public FirstPassGroupingCollector(GroupSelector<T> groupSelector, Sort groupSort, int topNGroups) {
    // groupSelector用来说明我们统计的分组对象
    this.groupSelector = groupSelector;
    if (topNGroups < 1) {
      throw new IllegalArgumentException("topNGroups must be >= 1 (got " + topNGroups + ")");
    }

    // TODO: allow null groupSort to mean "by relevance",
    // and specialize it?

    this.topNGroups = topNGroups;
    // groupSort的排序规则如果根据score，那么needsScores的值为true
    this.needsScores = groupSort.needsScores();
    final SortField[] sortFields = groupSort.getSort();
    // comparators是一个数组，即可以使用多个排序规则
    comparators = new FieldComparator[sortFields.length];
    leafComparators = new LeafFieldComparator[sortFields.length];
    // 设置遍历次数(排序器的个数 - 1)
    compIDXEnd = comparators.length - 1;
    // 定义每一个排序的规则是否为倒序
    reversed = new int[sortFields.length];

    for (int i = 0; i < sortFields.length; i++) {
      final SortField sortField = sortFields[i];

      // use topNGroups + 1 so we have a spare slot to use for comparing (tracked by this.spareSlot):
      // 每一个排序器的大小为topNGroups + 1，目的是多出一个位置来存储某个值，这个值记录了可以被替换的下标
      // 取出每一个排序器
      comparators[i] = sortField.getComparator(topNGroups + 1, i);
      // 设置每一个排序的排序规则之一：是否倒序
      reversed[i] = sortField.getReverse() ? -1 : 1;
    }

    spareSlot = topNGroups;
    groupMap = new HashMap<>(topNGroups);
  }

  @Override
  public boolean needsScores() {
    return needsScores;
  }

  /**
   * Returns top groups, starting from offset.  This may
   * return null, if no groups were collected, or if the
   * number of unique groups collected is &lt;= offset.
   *
   * @param groupOffset The offset in the collected groups
   * @param fillFields Whether to fill to {@link SearchGroup#sortValues}
   * @return top groups, starting from offset
   */
  public Collection<SearchGroup<T>> getTopGroups(int groupOffset, boolean fillFields) throws IOException {

    //System.out.println("FP.getTopGroups groupOffset=" + groupOffset + " fillFields=" + fillFields + " groupMap.size()=" + groupMap.size());

    if (groupOffset < 0) {
      throw new IllegalArgumentException("groupOffset must be >= 0 (got " + groupOffset + ")");
    }

    if (groupMap.size() <= groupOffset) {
      return null;
    }

    // if语句为真说明收集到的个数没有达到TopN
    if (orderedGroups == null) {
      buildSortedSet();
    }

    final Collection<SearchGroup<T>> result = new ArrayList<>();
    int upto = 0;
    final int sortFieldCount = comparators.length;
    // 下面的for循环实现了 获取每一个groupValue的 值 跟 比较值(用来区分不同groupValue大小的值)
    for(CollectedSearchGroup<T> group : orderedGroups) {
      if (upto++ < groupOffset) {
        continue;
      }
      // System.out.println("  group=" + (group.groupValue == null ? "null" : group.groupValue.toString()));
      SearchGroup<T> searchGroup = new SearchGroup<>();
      // 存放groupValue的值
      searchGroup.groupValue = group.groupValue;
      if (fillFields) {
        searchGroup.sortValues = new Object[sortFieldCount];
        for(int sortFieldIDX=0;sortFieldIDX<sortFieldCount;sortFieldIDX++) {
          // 将每个comparator中用于比较的值存放到Object[]对象中
          searchGroup.sortValues[sortFieldIDX] = comparators[sortFieldIDX].value(group.comparatorSlot);
        }
      }
      result.add(searchGroup);
    }
    //System.out.println("  return " + result.size() + " groups");
    return result;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    // 不用的比较器提取scorer不同的数据作为比较的原始值
    for (LeafFieldComparator comparator : leafComparators) {
      comparator.setScorer(scorer);
    }
  }

  private boolean isCompetitive(int doc) throws IOException {
    // If orderedGroups != null we already have collected N groups and
    // can short circuit by comparing this document to the bottom group,
    // without having to find what group this document belongs to.

    // Even if this document belongs to a group in the top N, we'll know that
    // we don't have to update that group.

    // Downside: if the number of unique groups is very low, this is
    // wasted effort as we will most likely be updating an existing group.
    // if条件为真表示我们已经收集到了topN的groups, orderedGroups存放了排序的groupValue的信息(CollectedSearchGroup对象)
    if (orderedGroups != null) {
      for (int compIDX = 0;; compIDX++) {
        final int c = reversed[compIDX] * leafComparators[compIDX].compareBottom(doc);
        if (c < 0) {
          // Definitely not competitive. So don't even bother to continue
          return false;
        } else if (c > 0) {
          // Definitely competitive.
          break;
        } else if (compIDX == compIDXEnd) {
          // Here c=0. If we're at the last comparator, this doc is not
          // competitive, since docs are visited in doc Id order, which means
          // this doc cannot compete with any other document in the queue.
          // 如果无法用规则比较出大小，那么文档号大的被丢弃(在collect(int )方法中会直接return)
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void collect(int doc) throws IOException {

    if (isCompetitive(doc) == false)
      return;

    // TODO: should we add option to mean "ignore docs that
    // don't have the group field" (instead of stuffing them
    // under null group)?
    // 根据doc值去SortedDocValues中找到对应的域值，也就是groupValue的值
    groupSelector.advanceTo(doc);
    // 获得当前group的域值
    T groupValue = groupSelector.currentValue();

    // 检查当前groupValue是否之前已经收集过
    final CollectedSearchGroup<T> group = groupMap.get(groupValue);

    // if语句为真说明第一次处理这个groupValue, 或者是这个groupValue处理过但是被踢出了TopN
    if (group == null) {

      // First time we are seeing this group, or, we've seen
      // it before but it fell out of the top N and is now
      // coming back

      // if语句为真说明处理的groupValue种类还没有超过TopN
      if (groupMap.size() < topNGroups) {

        // Still in startup transient: we have not
        // seen enough unique groups to start pruning them;
        // just keep collecting them

        // Add a new CollectedSearchGroup:
        // 新建一个对象来描述groupValue的一些信息
        CollectedSearchGroup<T> sg = new CollectedSearchGroup<>();
        // 记录groupValue的值(深拷贝，即新对象)
        sg.groupValue = groupSelector.copyValue();
        // 记录当前groupValue的comparatorSlot，因为还没有收集满，所以另其为groupMap.size()的值
        sg.comparatorSlot = groupMap.size();
        // 实际的文档号
        sg.topDoc = docBase + doc;
        for (LeafFieldComparator fc : leafComparators) {
          fc.copy(sg.comparatorSlot, doc);
        }
        // 记录这个groupValue的CollectedSearchGroup对象
        groupMap.put(sg.groupValue, sg);

        if (groupMap.size() == topNGroups) {
          // End of startup transient: we now have max
          // number of groups; from here on we will drop
          // bottom group when we insert new one:
          // 当收集满TopN的groupValue时，我们要开始进行排序
          // 目的是找到 最小/最大（基于规则）,用于跟新的groupValue进行比较
          buildSortedSet();
        }

        return;
      }

      // We already tested that the document is competitive, so replace
      // the bottom group with this new group.
      // 当前这个新的groupValue具有competitive，后面的逻辑替换掉orderedGroups中最小(基于规则)的那个
      // 取出（移除）最小(基于规则)的那个元素
      final CollectedSearchGroup<T> bottomGroup = orderedGroups.pollLast();
      assert orderedGroups.size() == topNGroups -1;

      // 同时在groupMap中移除最小的那个
      groupMap.remove(bottomGroup.groupValue);

      // reuse the removed CollectedSearchGroup
      // 复用orderedGroups中那个最小(基于规则)的元素
      bottomGroup.groupValue = groupSelector.copyValue();
      bottomGroup.topDoc = docBase + doc;

      for (LeafFieldComparator fc : leafComparators) {
        fc.copy(bottomGroup.comparatorSlot, doc);
      }

      // 更新groupMap的值
      groupMap.put(bottomGroup.groupValue, bottomGroup);
      // 重新添加到orderedGroups中，并且重新排序
      orderedGroups.add(bottomGroup);
      assert orderedGroups.size() == topNGroups;

      /** 重新获得在orderedGroups中最小(基于规则)的元素的comparatorSlot的值，目的是更新FieldComparator中的 {@link LeafFieldComparator} 中的 bottom的值 */
      // 更新后的bottom用于后面的比较
      final int lastComparatorSlot = orderedGroups.last().comparatorSlot;
      for (LeafFieldComparator fc : leafComparators) {
        fc.setBottom(lastComparatorSlot);
      }

      return;
    }

    // Update existing group:
    // 更新已经存在的groupValue的信息
    for (int compIDX = 0;; compIDX++) {
      // 将文档号赋值到在下标为spareSlot的位置, 比如将doc的值赋值给RelevanceComparator对象中的下标为spareSlot的scorers数组元素
      leafComparators[compIDX].copy(spareSlot, doc);

      // 比较两个groupValue，例子：文档打分为规则的比较器中，就是比较RelevanceComparator对象中的scorers[]数组中的值（文档打分）
      final int c = reversed[compIDX] * comparators[compIDX].compare(group.comparatorSlot, spareSlot);
      if (c < 0) {
        // Definitely not competitive.
        return;
      } else if (c > 0) {
        // Definitely competitive; set remaining comparators:
        for (int compIDX2=compIDX+1; compIDX2<comparators.length; compIDX2++) {
          leafComparators[compIDX2].copy(spareSlot, doc);
        }
        break;
      } else if (compIDX == compIDXEnd) {
        // Here c=0. If we're at the last comparator, this doc is not
        // competitive, since docs are visited in doc Id order, which means
        // this doc cannot compete with any other document in the queue.
        // 比较器无法给出比较结果，那么文档大的被丢弃
        return;
      }
    }

    // Remove before updating the group since lookup is done via comparators
    // TODO: optimize this

    final CollectedSearchGroup<T> prevLast;
    if (orderedGroups != null) {
      // 取出最小的(基于规则)的元素
      prevLast = orderedGroups.last();
      // 移除最小的(基于规则)的元素
      orderedGroups.remove(group);
      assert orderedGroups.size() == topNGroups-1;
    } else {
      prevLast = null;
    }

    // group对象复用，更新文档号
    group.topDoc = docBase + doc;

    // Swap slots
    // 更新spareSlot的值, 该值被赋值为刚刚被替换掉的groupValue之前在scorers[]中的下标
    final int tmp = spareSlot;
    spareSlot = group.comparatorSlot;
    group.comparatorSlot = tmp;

    // Re-add the changed group
    if (orderedGroups != null) {
      // 重新添加到orderedGroups
      orderedGroups.add(group);
      assert orderedGroups.size() == topNGroups;
      final CollectedSearchGroup<?> newLast = orderedGroups.last();
      // If we changed the value of the last group, or changed which group was last, then update bottom:
      // 插入到orderedGroups后，如果发生了排序的操作，那么就更新bottom的值
      if (group == newLast || prevLast != newLast) {
        for (LeafFieldComparator fc : leafComparators) {
          fc.setBottom(newLast.comparatorSlot);
        }
      }
    }
  }

  private void buildSortedSet() throws IOException {
    // 定义比较规则
    final Comparator<CollectedSearchGroup<?>> comparator = new Comparator<CollectedSearchGroup<?>>() {
      @Override
      public int compare(CollectedSearchGroup<?> o1, CollectedSearchGroup<?> o2) {
        // 这里使用for循环的目的是：如果第一个排序规则无法区分，那么就使用第二个排序规则
        for (int compIDX = 0;; compIDX++) {
          FieldComparator<?> fc = comparators[compIDX];
          final int c = reversed[compIDX] * fc.compare(o1.comparatorSlot, o2.comparatorSlot);
          if (c != 0) {
            return c;
            //满足这里的if条件说明，所有的规则（规则的个数是compIDXEnd）都比较过了，但是无法区分两者的大小，那么判断文档号
          } else if (compIDX == compIDXEnd) {
            return o1.topDoc - o2.topDoc;
          }
        }
      }
    };

    // 使用TreeSet实现排序
    orderedGroups = new TreeSet<>(comparator);
    // 排序的过程在添加元素时进行
    orderedGroups.addAll(groupMap.values());
    assert orderedGroups.size() > 0;

    for (LeafFieldComparator fc : leafComparators) {
      fc.setBottom(orderedGroups.last().comparatorSlot);
    }
  }

  @Override
  protected void doSetNextReader(LeafReaderContext readerContext) throws IOException {
    docBase = readerContext.docBase;
    for (int i=0; i<comparators.length; i++) {
      leafComparators[i] = comparators[i].getLeafComparator(readerContext);
    }
    groupSelector.setNextReader(readerContext);
  }

  /**
   * @return the GroupSelector used for this Collector
   */
  public GroupSelector<T> getGroupSelector() {
    return groupSelector;
  }

}

