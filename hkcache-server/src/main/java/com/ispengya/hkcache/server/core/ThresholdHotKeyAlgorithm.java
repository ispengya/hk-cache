package com.ispengya.hkcache.server.core;

import java.util.HashSet;
import java.util.Set;

/**
 * ThresholdHotKeyAlgorithm 基于固定阈值的热 Key 计算算法。
 *
 * <p>若某个 key 在统计窗口内的访问总次数超过设定阈值，则判定为热 Key。</p>
 *
 * @author ispengya
 */
public final class ThresholdHotKeyAlgorithm implements HotKeyComputeAlgorithm {

    /**
     * 最小访问次数阈值。
     */
    private final long minCountThreshold;

    /**
     * 构造阈值算法。
     *
     * @param minCountThreshold 访问次数阈值
     */
    public ThresholdHotKeyAlgorithm(long minCountThreshold) {
        this.minCountThreshold = minCountThreshold;
    }

    @Override
    public Set<String> computeHotKeys(Iterable<AggregatedKeyStat> stats) {
        Set<String> result = new HashSet<>();
        for (AggregatedKeyStat stat : stats) {
            if (stat.getTotalCount() >= minCountThreshold) {
                result.add(stat.getKey());
            }
        }
        return result;
    }
}
