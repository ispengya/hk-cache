package com.ispengya.hotkey.cli.origin;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DefaultSafeLoadExecutor 是 SafeLoadExecutor 的默认实现，
 * 内部基于本地 ConcurrentHashMap 管理 instanceName + key 维度的锁。
 *
 * 每个实例下的同一 key 共享一把可重入锁，用于串行回源。
 *
 * @author ispengya
 */
public class DefaultSafeLoadExecutor implements SafeLoadExecutor {

    private final ConcurrentMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Override
    public <T> T safeLoad(String instanceName, String key, Callable<T> loader) throws Exception {
        ReentrantLock lock = acquireLock(instanceName, key);
        try {
            return loader.call();
        } finally {
            releaseLock(instanceName, lock);
        }
    }

    @Override
    public ReentrantLock acquireLock(String instanceName, String key) {
        String lockKey = buildLockKey(instanceName, key);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    @Override
    public void releaseLock(String instanceName, ReentrantLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

    private String buildLockKey(String instanceName, String key) {
        String inst = instanceName == null ? "default" : instanceName;
        String k = key == null ? "" : key;
        return inst + "|" + k;
    }
}
