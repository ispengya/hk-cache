package com.ispengya.hkcache.server.core;

import com.ispengya.hkcache.server.model.HotKeyResult;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * InMemoryHotKeyResultStore 基于内存的热 Key 结果存储实现。
 *
 * @author ispengya
 */
public final class InMemoryHotKeyResultStore implements HotKeyResultStore {

    /**
     * 实例热 Key 结果映射：instanceId -> HotKeyResult。
     */
    private final ConcurrentMap<String, HotKeyResult> store = new ConcurrentHashMap<>();

    @Override
    public void update(HotKeyResult result) {
        store.put(result.getInstanceId(), result);
    }

    @Override
    public HotKeyResult get(String instanceId) {
        return store.get(instanceId);
    }

    @Override
    public Iterable<HotKeyResult> listAll() {
        return store.values();
    }
}
