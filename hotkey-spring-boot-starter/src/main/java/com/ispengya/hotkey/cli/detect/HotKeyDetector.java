package com.ispengya.hotkey.cli.detect;

import com.ispengya.hkcache.remoting.client.HotKeyRemotingClient;
import com.ispengya.hkcache.remoting.message.AccessReportMessage;
import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import com.ispengya.hotkey.cli.config.InstanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * HotKeyDetector 负责采集访问轨迹或上报数据，
 * 并定期从 Server 拉取最新的热 Key 集合。
 *
 * @author ispengya
 */
public class HotKeyDetector {

    private static final Logger log = LoggerFactory.getLogger(HotKeyDetector.class);

    private final String instanceId;
    private final HotKeyRemotingClient remotingClient;
    private final HotKeySet hotKeySet;
    private final ScheduledExecutorService scheduler;

    // 暂存访问计数的 Buffer: key -> count
    private final ConcurrentHashMap<String, LongAdder> accessBuffer = new ConcurrentHashMap<>();

    // 配置参数（后续可抽取到 Properties）
    private final long reportPeriodMillis = 5000L;
    private final long queryPeriodMillis = 1000L;
    private final long queryTimeoutMillis = 3000L;

    public HotKeyDetector(InstanceConfig instanceConfig,
                          HotKeyRemotingClient remotingClient,
                          HotKeySet hotKeySet) {
        this.instanceId = instanceConfig.getInstanceName(); // 假设 InstanceConfig 有 getInstanceName
        this.remotingClient = remotingClient;
        this.hotKeySet = hotKeySet;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public void start() {
        // 定时上报任务
        scheduler.scheduleAtFixedRate(this::reportTask, reportPeriodMillis, reportPeriodMillis, TimeUnit.MILLISECONDS);
        // 定时拉取任务
        scheduler.scheduleAtFixedRate(this::queryTask, queryPeriodMillis, queryPeriodMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    /**
     * 记录一次对指定 key 的访问。
     *
     * @param key 访问的业务 key
     */
    public void recordAccess(String key) {
        if (key == null) {
            return;
        }
        accessBuffer.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    private void reportTask() {
        try {
            if (accessBuffer.isEmpty()) {
                return;
            }

            // 提取并清空当前 Buffer
            Map<String, Integer> counts = new HashMap<>();
            accessBuffer.forEach((key, adder) -> {
                long count = adder.sumThenReset();
                if (count > 0) {
                    counts.put(key, (int) count);
                }
            });
            // 清理 count 为 0 的 key，避免 map 无限膨胀（sumThenReset 后 adder 仍在 map 中）
            // 简单策略：如果 map 过大，整体重建。或者依赖 computeIfAbsent 的开销可控。
            // 生产环境建议使用更高效的结构或定期清理。这里简单做 remove。
            accessBuffer.entrySet().removeIf(e -> e.getValue().sum() == 0);

            if (counts.isEmpty()) {
                return;
            }

            AccessReportMessage message = new AccessReportMessage();
            message.setInstanceId(instanceId);
            message.setTimestamp(System.currentTimeMillis());
            message.setKeyAccessCounts(counts);

            remotingClient.reportAccess(message);

        } catch (Exception e) {
            log.error("Failed to report access data", e);
        }
    }

    private void queryTask() {
        try {
            long currentVersion = hotKeySet.getVersion();
            HotKeyViewMessage message = remotingClient.queryHotKeys(instanceId, currentVersion, queryTimeoutMillis);

            if (message != null && message.getVersion() > currentVersion) {
                hotKeySet.update(message.getHotKeys(), message.getVersion());
                if (log.isDebugEnabled()) {
                    log.debug("Updated hot keys to version {}, count: {}", message.getVersion(), message.getHotKeys().size());
                }
            }
        } catch (Exception e) {
            log.error("Failed to query hot keys", e);
        }
    }
}
