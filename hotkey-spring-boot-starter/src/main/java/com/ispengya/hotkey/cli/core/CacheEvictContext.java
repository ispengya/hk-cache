package com.ispengya.hotkey.cli.core;

/**
 * CacheEvictContext 描述一次缓存失效操作的上下文，
 * 主要包含实例名称、key 以及触发失效时的时间戳。
 *
 * 该上下文将被 CacheTemplate 用于执行精确或全量失效操作。
 *
 * @author ispengya
 */
public final class CacheEvictContext {

    private final String instanceName;
    private final String key;
    private final long nowMillis;

    /**
     * 构造缓存失效上下文。
     *
     * @param instanceName 实例名称
     * @param key          需要失效的缓存 key
     * @param nowMillis    触发失效的时间戳
     */
    public CacheEvictContext(String instanceName, String key, long nowMillis) {
        this.instanceName = instanceName;
        this.key = key;
        this.nowMillis = nowMillis;
    }

    /**
     * 获取实例名称。
     *
     * @return 实例名称
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * 获取需要失效的 key。
     *
     * @return 缓存 key
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取触发失效的时间戳。
     *
     * @return 毫秒级时间戳
     */
    public long getNowMillis() {
        return nowMillis;
    }
}

