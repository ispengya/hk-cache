package com.ispengya.hotkey.cli.origin;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SafeLoadExecutor 抽象基于本地锁的安全回源能力，
 * 用于在高并发场景下避免同一实例下对同一 key 的重复回源。
 *
 * 默认实现由 DefaultSafeLoadExecutor 提供，内部包含 instanceName + key 维度的锁管理。
 *
 * @author ispengya
 */
public interface SafeLoadExecutor {

    /**
     * 为指定实例和 key 获取或创建一个本地锁，并加锁。
     *
     * @param instanceName 实例名称
     * @param key          业务 key
     * @return 已加锁的 ReentrantLock
     */
    ReentrantLock acquireLock(String instanceName, String key);

    /**
     * 释放指定实例维度下的本地锁。
     *
     * @param instanceName 实例名称
     * @param lock         需要释放的锁对象
     */
    void releaseLock(String instanceName, ReentrantLock lock);

    /**
     * 使用本地锁保护回源逻辑，避免同一实例下同一 key 的并发击穿。
     *
     * @param instanceName 实例名称
     * @param key          业务 key
     * @param loader       回源逻辑
     * @param <T>          返回值类型
     * @return 回源结果
     * @throws Exception 当回源逻辑抛出异常时向上透传
     */
    <T> T safeLoad(String instanceName, String key, Callable<T> loader) throws Exception;
}
