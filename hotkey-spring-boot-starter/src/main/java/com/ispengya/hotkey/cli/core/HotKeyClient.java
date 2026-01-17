package com.ispengya.hotkey.cli.core;

import com.ispengya.hotkey.cli.detect.HotKeyDetector;
import com.ispengya.hotkey.cli.detect.HotKeySet;

/**
 * HotKeyClient 是对业务方暴露的统一访问入口，
 * 负责串联热 key 探测、本地缓存和回源等能力。
 *
 * 业务只需要提供 ValueLoader 回源逻辑，即可通过 get 方法完成访问。
 *
 * 客户端绑定实例名称，用于多实例隔离和本地锁粒度控制。
 *
 * @author ispengya
 */
public class HotKeyClient {

    private final String instanceName;
    private final HotKeyDetector hotKeyDetector;
    private final HotKeySet hotKeySet;
    private final CacheTemplate cacheTemplate;
    private final PostLoadAction postLoadAction;

    /**
     * 使用必要依赖构造 HotKeyClient。
     *
     * @param instanceName    实例名称
     * @param hotKeyDetector  热 key 探测器
     * @param hotKeySet       本地热 key 视图
     * @param cacheTemplate   缓存模板
     * @param postLoadAction  加载完成后的回调
     */
    public HotKeyClient(String instanceName,
                        HotKeyDetector hotKeyDetector,
                        HotKeySet hotKeySet,
                        CacheTemplate cacheTemplate,
                        PostLoadAction postLoadAction) {
        this.instanceName = instanceName;
        this.hotKeyDetector = hotKeyDetector;
        this.hotKeySet = hotKeySet;
        this.cacheTemplate = cacheTemplate;
        this.postLoadAction = postLoadAction;
    }

    /**
     * 统一访问入口：记录访问、判断是否热 key、走缓存模板并上报结果。
     *
     * @param key    业务 key
     * @param loader 回源逻辑
     * @param <T>    返回值类型
     * @return 业务数据结果
     */
    public <T> T get(String key, ValueLoader<T> loader) {
        long startNs = System.nanoTime();

        hotKeyDetector.recordAccess(key);

        boolean isHot = hotKeySet.contains(key);

        CacheableContext context = new CacheableContext(instanceName, key, isHot, System.currentTimeMillis());

        CacheResult<T> result = cacheTemplate.load(context, loader);

        postLoadAction.onLoaded(key, result.getValue(), result.isFromCache(), result.getCostNanos());

        return result.getValue();
    }

    /**
     * 失效当前实例下指定 key 的本地缓存。
     *
     * @param key 需要失效的业务 key
     */
    public void evict(String key) {
        CacheEvictContext context = new CacheEvictContext(
                instanceName,
                key,
                System.currentTimeMillis()
        );
        cacheTemplate.evict(context);
    }

    /**
     * 清空当前实例对应的全部本地缓存。
     * 当前实现为全局清空，不按实例进一步区分。
     */
    public void evictAll() {
        cacheTemplate.evictAll(instanceName);
    }

    /**
     * 获取当前客户端绑定的实例名称。
     *
     * @return 实例名称
     */
    public String getInstanceName() {
        return instanceName;
    }
}
