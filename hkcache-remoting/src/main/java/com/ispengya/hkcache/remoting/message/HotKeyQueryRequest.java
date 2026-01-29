package com.ispengya.hkcache.remoting.message;

import java.io.Serializable;
import java.util.Map;

/**
 * HotKeyQueryRequest 表示 CLI 向 server 端发起的热 key 查询请求。
 *
 * <p>请求中包含实例 ID 以及上一次收到的版本号，server 可以根据版本号决定
 * 是否返回最新热 key 视图。</p>
 */
public class HotKeyQueryRequest implements Serializable {

    /**
     * 发起查询的实例 ID。
     */
    private String instanceId;

    /**
     * 上一次接收到的热 key 视图版本号。
     */
    private long lastVersion;
    private Map<String, Long> lastVersions;

    /**
     * 无参构造函数，便于序列化框架使用。
     */
    public HotKeyQueryRequest() {
    }

    /**
     * 构造热 key 查询请求。
     *
     * @param instanceId  实例 ID
     * @param lastVersion 上一次接收的版本号
     */
    public HotKeyQueryRequest(String instanceId, long lastVersion) {
        this.instanceId = instanceId;
        this.lastVersion = lastVersion;
    }

    public HotKeyQueryRequest(Map<String, Long> lastVersions) {
        this.lastVersions = lastVersions;
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
     * 获取上一次接收的版本号。
     */
    public long getLastVersion() {
        return lastVersion;
    }

    /**
     * 设置上一次接收的版本号。
     */
    public void setLastVersion(long lastVersion) {
        this.lastVersion = lastVersion;
    }

    public Map<String, Long> getLastVersions() {
        return lastVersions;
    }

    public void setLastVersions(Map<String, Long> lastVersions) {
        this.lastVersions = lastVersions;
    }
}
