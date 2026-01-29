package com.ispengya.hkcache.remoting.message;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * HotKeyViewMessage 表示 server 端计算出的热 key 视图。
 *
 * <p>CLI 周期性向 server 端发起 HOT_KEY_QUERY 请求后，server 将当前版本的
 * 热 key 集合封装在该消息中返回。</p>
 */
public class HotKeyViewMessage implements Serializable {

    public static final class ViewEntry implements Serializable {
        private long version;
        private Set<String> hotKeys;

        public ViewEntry() {
        }

        public ViewEntry(long version, Set<String> hotKeys) {
            this.version = version;
            this.hotKeys = hotKeys;
        }

        public long getVersion() {
            return version;
        }

        public void setVersion(long version) {
            this.version = version;
        }

        public Set<String> getHotKeys() {
            return hotKeys;
        }

        public void setHotKeys(Set<String> hotKeys) {
            this.hotKeys = hotKeys;
        }
    }

    /**
     * 对应的实例 ID。
     */
    private String instanceId;

    /**
     * 热 key 结果的版本号，用于增量拉取或幂等判断。
     */
    private long version;

    /**
     * 当前判定为热 key 的 key 集合。
     */
    private Set<String> hotKeys;
    private Map<String, ViewEntry> views;

    /**
     * 无参构造函数，便于序列化框架使用。
     */
    public HotKeyViewMessage() {
    }

    /**
     * 构造热 key 视图。
     *
     * @param instanceId 实例 ID
     * @param version    结果版本号
     * @param hotKeys    热 key 集合
     */
    public HotKeyViewMessage(String instanceId, long version, Set<String> hotKeys) {
        this.instanceId = instanceId;
        this.version = version;
        this.hotKeys = hotKeys;
    }

    /**
     * 获取实例 ID。
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 设置实例 ID。
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 获取结果版本号。
     */
    public long getVersion() {
        return version;
    }

    /**
     * 设置结果版本号。
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * 获取热 key 集合。
     */
    public Set<String> getHotKeys() {
        return hotKeys;
    }

    /**
     * 设置热 key 集合。
     */
    public void setHotKeys(Set<String> hotKeys) {
        this.hotKeys = hotKeys;
    }

    public Map<String, ViewEntry> getViews() {
        return views;
    }

    public void setViews(Map<String, ViewEntry> views) {
        this.views = views;
    }
}
