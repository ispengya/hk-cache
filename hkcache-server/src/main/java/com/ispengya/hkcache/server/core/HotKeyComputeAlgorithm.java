package com.ispengya.hkcache.server.core;

import java.util.Set;

/**
 * HotKeyComputeAlgorithm 定义热 Key 计算算法接口。
 *
 * <p>输入一组聚合后的统计数据，输出被判定为热 Key 的集合。</p>
 *
 * @author ispengya
 */
public interface HotKeyComputeAlgorithm {

    /**
     * 执行热 Key 计算。
     *
     * @param stats 聚合统计数据
     * @return 热 Key 集合
     */
    Set<String> computeHotKeys(Iterable<AggregatedKeyStat> stats);
}
