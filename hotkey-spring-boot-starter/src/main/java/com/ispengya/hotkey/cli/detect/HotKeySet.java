package com.ispengya.hotkey.cli.detect;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HotKeySet 表示本地维护的热 key 集合，
 * 通常由服务端定期下发并覆盖更新。
 *
 * 业务方可以通过该集合判断某个 key 当前是否为热 key。
 *
 * @author ispengya
 */
public class HotKeySet {

    private volatile Set<String> hotKeys = Collections.emptySet();
    private final AtomicLong version = new AtomicLong(0);

    /**
     * 使用新的热 key 集合整体替换当前视图。
     *
     * @param keys 新的热 key 集合
     * @param newVersion 新的版本号
     */
    public synchronized boolean update(Iterable<String> keys, long newVersion) {
        long current = version.get();
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
        this.hotKeys = Collections.unmodifiableSet(newSet);
        this.version.set(newVersion);
        return true;
    }

    /**
     * 判断指定 key 是否当前被视为热 key。
     *
     * @param key 业务 key
     * @return true 表示热 key
     */
    public boolean contains(String key) {
        return hotKeys.contains(key);
    }

    /**
     * 获取当前热 key 集合的版本号。
     *
     * @return 版本号
     */
    public long getVersion() {
        return version.get();
    }
}
