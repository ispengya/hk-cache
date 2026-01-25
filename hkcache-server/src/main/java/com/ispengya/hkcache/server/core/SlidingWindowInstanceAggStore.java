package com.ispengya.hkcache.server.core;

import com.ispengya.hkcache.server.model.AccessReport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        long now = report.getTimestampMillis();
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
        Map<String, AggregatedKeyStat> merged = new HashMap<>();
        for (WindowSlot slot : slots) {
            // 对槽内容进行快照，避免迭代期间发生并发修改
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
        // 简化逻辑：仅返回第一个槽，或实现实际的轮转逻辑。
        // 设计文档伪代码为：return slots.get(0);
        // 若需真实滑动窗口，需根据时间计算索引。
        // 目前暂按文档实现，后续可扩展为 Ring Buffer。
        return slots.get(0); 
    }
}
