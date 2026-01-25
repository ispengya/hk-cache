package com.ispengya.hkcache.server.core;

/**
 * HotKeyAggregator 负责管理所有实例的 {@link InstanceAggStore}。
 *
 * <p>它是聚合层的入口，确保每个实例的数据被路由到正确的存储单元。</p>
 *
 * @author ispengya
 */
public interface HotKeyAggregator {

    /**
     * 获取或创建指定实例的聚合存储。
     *
     * @param instanceId 实例 ID
     * @return 对应的 InstanceAggStore
     */
    InstanceAggStore getOrCreateStore(String instanceId);
}
