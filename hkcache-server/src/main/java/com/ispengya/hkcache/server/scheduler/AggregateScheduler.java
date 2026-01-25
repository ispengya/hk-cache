package com.ispengya.hkcache.server.scheduler;

import com.ispengya.hkcache.server.core.HotKeyAggregateService;
import com.ispengya.hkcache.server.core.HotKeyComputeAlgorithm;
import com.ispengya.hkcache.server.core.HotKeyResultStore;
import com.ispengya.hkcache.server.core.InstanceRegistry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AggregateScheduler 聚合调度器。
 *
 * <p>定期触发热 Key 计算任务，遍历所有活跃实例并提交 {@link HotKeyComputeTask} 到工作线程池。</p>
 *
 * @author ispengya
 */
public final class AggregateScheduler {

    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;
    private final HotKeyAggregateService aggregateService;
    private final HotKeyComputeAlgorithm algorithm;
    private final HotKeyResultStore resultStore;
    private final InstanceRegistry instanceRegistry;
    private final long periodMillis;

    /**
     * 构造调度器。
     *
     * @param scheduler        定时调度线程池
     * @param workerPool       计算工作线程池
     * @param aggregateService 聚合服务
     * @param algorithm        计算算法
     * @param resultStore      结果存储
     * @param instanceRegistry 实例注册表
     * @param periodMillis     调度周期（毫秒）
     */
    public AggregateScheduler(ScheduledExecutorService scheduler,
                              ExecutorService workerPool,
                              HotKeyAggregateService aggregateService,
                              HotKeyComputeAlgorithm algorithm,
                              HotKeyResultStore resultStore,
                              InstanceRegistry instanceRegistry,
                              long periodMillis) {
        this.scheduler = scheduler;
        this.workerPool = workerPool;
        this.aggregateService = aggregateService;
        this.algorithm = algorithm;
        this.resultStore = resultStore;
        this.instanceRegistry = instanceRegistry;
        this.periodMillis = periodMillis;
    }

    /**
     * 启动调度器。
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
                this::scheduleComputeTasks,
                periodMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 停止调度器。
     */
    public void stop() {
        scheduler.shutdown();
        workerPool.shutdown();
    }

    private void scheduleComputeTasks() {
        for (String instanceId : instanceRegistry.listInstanceIds()) {
            HotKeyComputeTask task = new HotKeyComputeTask(
                    instanceId,
                    aggregateService,
                    algorithm,
                    resultStore
            );
            workerPool.submit(task);
        }
    }
}
