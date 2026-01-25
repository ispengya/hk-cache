package com.ispengya.hkcache.server.core;

import com.ispengya.hkcache.server.model.AccessReport;

/**
 * InstanceAggStore 定义单个实例维度的聚合存储接口。
 *
 * <p>负责接收该实例的上报数据，并提供当前时间窗口内的聚合统计快照。</p>
 *
 * @author ispengya
 */
public interface InstanceAggStore {

    /**
     * 添加一条访问上报记录。
     *
     * @param report 访问上报对象
     */
    void addReport(AccessReport report);

    /**
     * 获取当前聚合结果快照。
     *
     * @return 聚合统计列表
     */
    Iterable<AggregatedKeyStat> snapshot();
}
