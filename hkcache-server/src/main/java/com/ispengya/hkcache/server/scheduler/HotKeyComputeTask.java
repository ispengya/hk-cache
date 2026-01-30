package com.ispengya.hkcache.server.scheduler;

import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.CommandType;
import com.ispengya.hkcache.remoting.protocol.Serializer;
import com.ispengya.hkcache.remoting.server.ServerChannelManager;
import com.ispengya.hkcache.server.core.AggregatedKeyStat;
import com.ispengya.hkcache.server.core.HotKeyAggregateService;
import com.ispengya.hkcache.server.core.HotKeyComputeAlgorithm;
import com.ispengya.hkcache.server.core.HotKeyResultStore;
import com.ispengya.hkcache.server.model.HotKeyResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class HotKeyComputeTask implements Runnable {

    private static final ConcurrentMap<String, InstanceHotState> STATE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Lock> LOCKS = new ConcurrentHashMap<>();

    private final String instanceId;
    private final HotKeyAggregateService aggregateService;
    private final HotKeyComputeAlgorithm algorithm;
    private final HotKeyResultStore resultStore;
    private final ServerChannelManager channelManager;
    private final Serializer serializer;

    public HotKeyComputeTask(String instanceId,
                             HotKeyAggregateService aggregateService,
                             HotKeyComputeAlgorithm algorithm,
                             HotKeyResultStore resultStore,
                             ServerChannelManager channelManager,
                             Serializer serializer) {
        this.instanceId = instanceId;
        this.aggregateService = aggregateService;
        this.algorithm = algorithm;
        this.resultStore = resultStore;
        this.channelManager = channelManager;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        Lock lock = acquireInstanceLock(instanceId);
        lock.lock();
        try {
            Iterable<AggregatedKeyStat> stats = aggregateService.snapshot(instanceId);
            HotKeyResult previous = resultStore.get(instanceId);
            Set<String> previousKeys = previous == null ? Collections.emptySet() : previous.getHotKeys();

            Set<String> discovered = algorithm.computeHotKeys(stats);
            if (discovered == null || discovered.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            for (String key : discovered) {
                if (key == null) {
                    continue;
                }
                recordHot(instanceId, key, 1.0d, now);
            }

            Set<String> newHotKeys = new HashSet<>(previousKeys);
            newHotKeys.addAll(discovered);

            Set<String> addedKeys = new HashSet<>(discovered);
            addedKeys.removeAll(previousKeys);
            if (addedKeys.isEmpty()) {
                return;
            }

            HotKeyResult result = HotKeyResult.from(instanceId, newHotKeys);
            resultStore.update(result);

            HotKeyViewMessage view = new HotKeyViewMessage();
            view.setInstanceId(result.getInstanceId());
            view.setVersion(result.getVersion());
            HotKeyViewMessage.ViewEntry entry = new HotKeyViewMessage.ViewEntry();
            entry.setVersion(result.getVersion());
            entry.setHotKeys(result.getHotKeys());
            entry.setAddedKeys(addedKeys);
            entry.setRemovedKeys(null);
            HashMap<String, HotKeyViewMessage.ViewEntry> views = new HashMap<>();
            views.put(result.getInstanceId(), entry);
            view.setViews(views);
            byte[] payload = serializer.serialize(view);
            Command command = new Command(CommandType.HOT_KEY_PUSH, 0L, payload);
            channelManager.broadcastOnPushChannels(command);
        } finally {
            lock.unlock();
        }
    }

    static long getLastActiveTime(String instanceId, String key) {
        InstanceHotState state = STATE.get(instanceId);
        if (state == null) {
            return 0L;
        }
        HotKeyEntry entry = state.entries.get(key);
        if (entry == null) {
            return 0L;
        }
        return entry.lastActiveTimeMillis;
    }

    static void removeKey(String instanceId, String key) {
        InstanceHotState state = STATE.get(instanceId);
        if (state == null) {
            return;
        }
        state.entries.remove(key);
    }

    private static void recordHot(String instanceId, String key, double deltaScore, long nowMillis) {
        InstanceHotState state = STATE.computeIfAbsent(instanceId, InstanceHotState::new);
        HotKeyEntry entry = state.entries.computeIfAbsent(key, HotKeyEntry::new);
        entry.score += deltaScore;
        entry.lastActiveTimeMillis = nowMillis;
    }

    static Lock acquireInstanceLock(String instanceId) {
        return LOCKS.computeIfAbsent(instanceId, k -> new ReentrantLock());
    }

    private static final class InstanceHotState {

        private final String instanceId;
        private final ConcurrentMap<String, HotKeyEntry> entries = new ConcurrentHashMap<>();

        private InstanceHotState(String instanceId) {
            this.instanceId = instanceId;
        }
    }

    private static final class HotKeyEntry {

        private final String key;
        private double score;
        private volatile long lastActiveTimeMillis;

        private HotKeyEntry(String key) {
            this.key = key;
        }
    }
}

final class HotKeyDecayTask implements Runnable {

    private final String instanceId;
    private final HotKeyResultStore resultStore;
    private final long idleMillis;
    private final ServerChannelManager channelManager;
    private final Serializer serializer;

    HotKeyDecayTask(String instanceId,
                    HotKeyResultStore resultStore,
                    long idleMillis,
                    ServerChannelManager channelManager,
                    Serializer serializer) {
        this.instanceId = instanceId;
        this.resultStore = resultStore;
        this.idleMillis = idleMillis;
        this.channelManager = channelManager;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        Lock lock = HotKeyComputeTask.acquireInstanceLock(instanceId);
        lock.lock();
        try {
            HotKeyResult previous = resultStore.get(instanceId);
            if (previous == null) {
                return;
            }
            Set<String> previousKeys = previous.getHotKeys();
            if (previousKeys == null || previousKeys.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            Set<String> newHotKeys = new HashSet<>(previousKeys);
            Set<String> removedKeys = new HashSet<>();
            boolean changed = false;

            for (String key : previousKeys) {
                if (key == null) {
                    continue;
                }
                long lastActive = HotKeyComputeTask.getLastActiveTime(instanceId, key);
                if (lastActive <= 0L || now - lastActive >= idleMillis) {
                    newHotKeys.remove(key);
                    HotKeyComputeTask.removeKey(instanceId, key);
                    removedKeys.add(key);
                    changed = true;
                }
            }

            if (!changed) {
                return;
            }

            HotKeyResult result = HotKeyResult.from(instanceId, newHotKeys);
            resultStore.update(result);

            HotKeyViewMessage view = new HotKeyViewMessage();
            view.setInstanceId(result.getInstanceId());
            view.setVersion(result.getVersion());
            HotKeyViewMessage.ViewEntry entry = new HotKeyViewMessage.ViewEntry();
            entry.setVersion(result.getVersion());
            entry.setHotKeys(result.getHotKeys());
            entry.setAddedKeys(null);
            entry.setRemovedKeys(removedKeys);
            HashMap<String, HotKeyViewMessage.ViewEntry> views = new HashMap<>();
            views.put(result.getInstanceId(), entry);
            view.setViews(views);
            byte[] payload = serializer.serialize(view);
            Command command = new Command(CommandType.HOT_KEY_PUSH, 0L, payload);
            channelManager.broadcastOnPushChannels(command);
        } finally {
            lock.unlock();
        }
    }
}
