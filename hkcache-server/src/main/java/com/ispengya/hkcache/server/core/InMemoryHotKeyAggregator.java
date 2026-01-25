package com.ispengya.hkcache.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * InMemoryHotKeyAggregator 基于内存实现的聚合器。
 *
 * <p>同时实现了 {@link InstanceRegistry}，维护了当前活跃的所有实例聚合存储。</p>
 *
 * @author ispengya
 */
public final class InMemoryHotKeyAggregator implements HotKeyAggregator, InstanceRegistry {

    /**
     * 实例聚合存储映射：instanceId -> InstanceAggStore。
     */
    private final ConcurrentMap<String, InstanceAggStore> stores = new ConcurrentHashMap<>();

    private final long windowSizeMillis;
    private final int windowSlotCount;

    /**
     * 构造内存聚合器。
     *
     * @param windowSizeMillis 窗口槽时间大小
     * @param windowSlotCount  窗口槽数量
     */
    public InMemoryHotKeyAggregator(long windowSizeMillis,
                                    int windowSlotCount) {
        this.windowSizeMillis = windowSizeMillis;
        this.windowSlotCount = windowSlotCount;
    }

    @Override
    public InstanceAggStore getOrCreateStore(String instanceId) {
        return stores.computeIfAbsent(instanceId, this::createStore);
    }

    @Override
    public Set<String> listInstanceIds() {
        return stores.keySet();
    }

    private InstanceAggStore createStore(String instanceId) {
        List<WindowSlot> slots = new ArrayList<>();
        long now = System.currentTimeMillis();
        long alignedStart = now - (now % windowSizeMillis);
        for (int i = 0; i < windowSlotCount; i++) {
            long start = alignedStart - (long) i * windowSizeMillis;
            slots.add(new WindowSlot(start, new ConcurrentHashMap<>()));
        }
        return new SlidingWindowInstanceAggStore(windowSizeMillis, windowSlotCount, slots);
    }
}
