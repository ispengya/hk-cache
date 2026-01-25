package com.ispengya.hkcache.server.core;

import com.ispengya.hkcache.server.model.AccessReport;
import java.util.Set;

/**
 * HotKeyAggregateService 聚合服务。
 *
 * <p>封装对 {@link HotKeyAggregator} 的操作，提供数据录入和快照查询能力。</p>
 *
 * @author ispengya
 */
public final class HotKeyAggregateService {

    private final HotKeyAggregator aggregator;

    public HotKeyAggregateService(HotKeyAggregator aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * 录入访问上报数据。
     *
     * @param report 访问上报对象
     */
    public void record(AccessReport report) {
        InstanceAggStore store = aggregator.getOrCreateStore(report.getInstanceId());
        store.addReport(report);
    }

    /**
     * 获取指定实例的聚合统计快照。
     *
     * @param instanceId 实例 ID
     * @return 聚合统计列表
     */
    public Iterable<AggregatedKeyStat> snapshot(String instanceId) {
        InstanceAggStore store = aggregator.getOrCreateStore(instanceId);
        return store.snapshot();
    }
}
