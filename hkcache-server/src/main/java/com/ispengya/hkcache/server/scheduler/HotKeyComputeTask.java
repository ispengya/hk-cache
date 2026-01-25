package com.ispengya.hkcache.server.scheduler;

import com.ispengya.hkcache.server.core.AggregatedKeyStat;
import com.ispengya.hkcache.server.core.HotKeyAggregateService;
import com.ispengya.hkcache.server.core.HotKeyComputeAlgorithm;
import com.ispengya.hkcache.server.core.HotKeyResultStore;
import com.ispengya.hkcache.server.model.HotKeyResult;
import java.util.Set;

/**
 * HotKeyComputeTask 热 Key 计算任务。
 *
 * <p>负责执行单个实例的热 Key 计算流程：
 * 1. 从聚合服务获取数据快照
 * 2. 使用算法计算热 Key 集合
 * 3. 将结果更新到结果存储</p>
 *
 * @author ispengya
 */
public final class HotKeyComputeTask implements Runnable {

    private final String instanceId;
    private final HotKeyAggregateService aggregateService;
    private final HotKeyComputeAlgorithm algorithm;
    private final HotKeyResultStore resultStore;

    /**
     * 构造计算任务。
     *
     * @param instanceId       实例 ID
     * @param aggregateService 聚合服务
     * @param algorithm        计算算法
     * @param resultStore      结果存储
     */
    public HotKeyComputeTask(String instanceId,
                             HotKeyAggregateService aggregateService,
                             HotKeyComputeAlgorithm algorithm,
                             HotKeyResultStore resultStore) {
        this.instanceId = instanceId;
        this.aggregateService = aggregateService;
        this.algorithm = algorithm;
        this.resultStore = resultStore;
    }

    @Override
    public void run() {
        Iterable<AggregatedKeyStat> stats = aggregateService.snapshot(instanceId);
        Set<String> hotKeys = algorithm.computeHotKeys(stats);
        HotKeyResult result = HotKeyResult.from(instanceId, hotKeys);
        resultStore.update(result);
    }
}
