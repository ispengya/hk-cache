package com.ispengya.hkcache.server.core;

import com.ispengya.hkcache.server.model.AccessReport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SlidingWindowInstanceAggStore 基于简易滑动窗口实现的聚合存储。
 *
 * <p>将时间划分为多个 {@link WindowSlot}，新上报的数据写入当前时间对应的槽中。
 * 获取快照时，合并所有槽的数据。</p>
 *
 * @author ispengya
 */
public final class SlidingWindowInstanceAggStore implements InstanceAggStore {

    /**
     * 单个窗口槽的时间跨度（毫秒）。
     */
    private final long windowSizeMillis;

    /**
     * 窗口包含的槽数量。
     */
    private final int windowSlotCount;

    /**
     * 槽列表。
     */
    private final List<WindowSlot> slots;

    /**
     * 构造滑动窗口聚合存储。
     *
     * @param windowSizeMillis 槽时间跨度
     * @param windowSlotCount  槽数量
     * @param slots            槽列表
     */
    public SlidingWindowInstanceAggStore(long windowSizeMillis,
                                         int windowSlotCount,
                                         List<WindowSlot> slots) {
        this.windowSizeMillis = windowSizeMillis;
        this.windowSlotCount = windowSlotCount;
        this.slots = slots;
    }

    @Override
    public void addReport(AccessReport report) {
        long now = System.currentTimeMillis();
        WindowSlot slot = resolveSlot(now);
        
        // 简单的同步逻辑保证槽内线程安全
        // 在高并发场景下，可考虑使用 ConcurrentHashMap 和原子更新，
        // 此处遵循设计文档的简化实现，并增加同步块。
        
        Map<String, AggregatedKeyStat> stats = slot.getStats();
        synchronized (stats) {
            AggregatedKeyStat stat = stats.get(report.getKey());
            if (stat == null) {
                stat = new AggregatedKeyStat(
                        report.getKey(),
                        0L,
                        0L,
                        0L,
                        0L
                );
            }
            long totalCount = stat.getTotalCount() + report.getCount();
            long successCount = stat.getSuccessCount() + (report.isSuccess() ? report.getCount() : 0L);
            long failCount = stat.getFailCount() + (report.isSuccess() ? 0L : report.getCount());
            long totalRtMillis = stat.getTotalRtMillis() + report.getRtMillis();
            AggregatedKeyStat newStat = new AggregatedKeyStat(
                    report.getKey(),
                    totalCount,
                    successCount,
                    failCount,
                    totalRtMillis
            );
            stats.put(report.getKey(), newStat);
        }
    }

    @Override
    public Iterable<AggregatedKeyStat> snapshot() {
        // merged 存放当前时间窗口内所有槽合并后的统计结果
        Map<String, AggregatedKeyStat> merged = new HashMap<>();
        // 对当前时间对齐到窗口起点，例如 10:00:01.234 对齐为 10:00:01.000
        long nowMillis = System.currentTimeMillis();
        long currentWindowStart = alignToWindow(nowMillis);
        // 整个滑动窗口覆盖的最早时间起点
        long minWindowStart = currentWindowStart - (windowSlotCount - 1L) * windowSizeMillis;
        for (WindowSlot slot : slots) {
            long slotStart = slot.getWindowStartMillis();
            // 只统计当前滑动窗口范围内的槽，超出范围的视为过期数据
            if (slotStart < minWindowStart || slotStart > currentWindowStart) {
                continue;
            }
            // 对槽内容进行快照，避免在合并过程中被并发修改
            Map<String, AggregatedKeyStat> slotStats;
            synchronized (slot.getStats()) {
                slotStats = new HashMap<>(slot.getStats());
            }

            for (AggregatedKeyStat stat : slotStats.values()) {
                AggregatedKeyStat existing = merged.get(stat.getKey());
                if (existing == null) {
                    merged.put(stat.getKey(), stat);
                } else {
                    AggregatedKeyStat mergedStat = new AggregatedKeyStat(
                            stat.getKey(),
                            existing.getTotalCount() + stat.getTotalCount(),
                            existing.getSuccessCount() + stat.getSuccessCount(),
                            existing.getFailCount() + stat.getFailCount(),
                            existing.getTotalRtMillis() + stat.getTotalRtMillis()
                    );
                    merged.put(stat.getKey(), mergedStat);
                }
            }
        }
        return merged.values();
    }

    private WindowSlot resolveSlot(long nowMillis) {
        // 将时间戳对齐到窗口起点，用于确定所属的时间槽
        long windowStart = alignToWindow(nowMillis);
        // 记录当前最老槽的起始时间及其索引，用于需要复用槽时选择被覆盖的目标
        long oldestStart = Long.MAX_VALUE;
        int oldestIndex = -1;
        for (int i = 0; i < windowSlotCount; i++) {
            WindowSlot slot = slots.get(i);
            long slotStart = slot.getWindowStartMillis();
            // 如果找到了与当前时间对应的槽，直接复用
            if (slotStart == windowStart) {
                return slot;
            }
            // 记录起始时间最早的槽，用于后续滑动窗口覆盖
            if (slotStart < oldestStart) {
                oldestStart = slotStart;
                oldestIndex = i;
            }
        }
        // 如果没有找到匹配的槽，则复用最老的那个槽，将其重置为当前窗口
        int index = oldestIndex >= 0 ? oldestIndex : 0;
        WindowSlot newSlot = new WindowSlot(windowStart, new ConcurrentHashMap<>());
        slots.set(index, newSlot);
        return newSlot;
    }

    private long alignToWindow(long timestampMillis) {
        // 容错处理：窗口大小非法时直接返回原始时间戳
        if (windowSizeMillis <= 0L) {
            return timestampMillis;
        }
        // 将时间戳向下取整到窗口边界，例如 1234ms 对齐到 1000ms
        return timestampMillis - (timestampMillis % windowSizeMillis);
    }
}
