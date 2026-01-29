package com.ispengya.hotkey.cli.detect;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class HotKeySet {

    private static final class InstanceView {
        private volatile Set<String> hotKeys = Collections.emptySet();
        private final AtomicLong version = new AtomicLong(0);
    }

    private final ConcurrentHashMap<String, InstanceView> views = new ConcurrentHashMap<>();

    public synchronized boolean update(String instanceId, Iterable<String> keys, long newVersion) {
        if (instanceId == null) {
            return false;
        }
        InstanceView view = views.computeIfAbsent(instanceId, k -> new InstanceView());
        long current = view.version.get();
        if (newVersion <= current) {
            return false;
        }
        Set<String> newSet = new HashSet<>();
        if (keys != null) {
            for (String key : keys) {
                if (key != null) {
                    newSet.add(key);
                }
            }
        }
        view.hotKeys = Collections.unmodifiableSet(newSet);
        view.version.set(newVersion);
        return true;
    }

    public boolean contains(String instanceId, String key) {
        if (instanceId == null || key == null) {
            return false;
        }
        InstanceView view = views.get(instanceId);
        if (view == null) {
            return false;
        }
        return view.hotKeys.contains(key);
    }

    public long getVersion(String instanceId) {
        if (instanceId == null) {
            return 0L;
        }
        InstanceView view = views.get(instanceId);
        if (view == null) {
            return 0L;
        }
        return view.version.get();
    }

    public Map<String, Long> snapshotVersions() {
        Map<String, Long> versions = new HashMap<>();
        views.forEach((instanceId, view) -> versions.put(instanceId, view.version.get()));
        return versions;
    }
}
