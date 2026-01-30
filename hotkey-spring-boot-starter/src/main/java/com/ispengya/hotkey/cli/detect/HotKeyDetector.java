package com.ispengya.hotkey.cli.detect;

import com.ispengya.hkcache.remoting.client.HotKeyRemotingClient;
import com.ispengya.hkcache.remoting.message.AccessReportMessage;
import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
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

    private final HotKeyRemotingClient remotingClient;
    private final HotKeySet hotKeySet;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, LongAdder>> accessBuffer = new ConcurrentHashMap<>();

    // 配置参数（后续可抽取到 Properties）
    private final long reportPeriodMillis = 5000L;
    private final long queryPeriodMillis = 30000L;
    private final long queryTimeoutMillis = 3000L;

    public HotKeyDetector(HotKeyRemotingClient remotingClient,
                          HotKeySet hotKeySet) {
        this.remotingClient = remotingClient;
        this.hotKeySet = hotKeySet;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.remotingClient.setPushListener(this::handlePush);
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

    public void recordAccess(String instanceName, String key) {
        if (instanceName == null || key == null) {
            return;
        }
        accessBuffer
                .computeIfAbsent(instanceName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new LongAdder())
                .increment();
    }

    private void reportTask() {
        try {
            if (accessBuffer.isEmpty()) {
                return;
            }

            Map<String, Map<String, Integer>> snapshot = new HashMap<>();
            accessBuffer.forEach((instanceName, keyMap) -> {
                Map<String, Integer> counts = new HashMap<>();
                keyMap.forEach((key, adder) -> {
                    long count = adder.sumThenReset();
                    if (count > 0) {
                        counts.put(key, (int) count);
                    }
                });
                if (!counts.isEmpty()) {
                    snapshot.put(instanceName, counts);
                }
            });

            accessBuffer.forEach((instanceName, keyMap) -> {
                keyMap.entrySet().removeIf(e -> e.getValue().sum() == 0);
                if (keyMap.isEmpty()) {
                    accessBuffer.remove(instanceName, keyMap);
                }
            });

            if (snapshot.isEmpty()) {
                return;
            }

            long timestamp = System.currentTimeMillis();
            snapshot.forEach((instanceName, counts) -> {
                AccessReportMessage message = new AccessReportMessage();
                message.setInstanceId(instanceName);
                message.setTimestamp(timestamp);
                message.setKeyAccessCounts(counts);
                remotingClient.reportAccess(message);
            });

        } catch (Exception e) {
            log.error("Failed to report access data", e);
        }
    }


    private void queryTask() {
        try {
            Map<String, Long> versions = hotKeySet.snapshotVersions();
            HotKeyViewMessage message = remotingClient.queryAllHotKeys(versions, queryTimeoutMillis);
            handleMessage(message);
        } catch (Exception e) {
            log.error("Failed to query hot keys", e);
        }
    }

    private void handlePush(HotKeyViewMessage message) {
        if (message == null) {
            return;
        }
        handleMessage(message);
    }

    private void handleMessage(HotKeyViewMessage message) {
        if (message == null) {
            return;
        }
        Map<String, HotKeyViewMessage.ViewEntry> views = message.getViews();
        if (views != null && !views.isEmpty()) {
            views.forEach((instanceId, entry) -> {
                if (entry == null) {
                    return;
                }
                boolean updated = false;
                if (entry.getAddedKeys() != null || entry.getRemovedKeys() != null) {
                    updated = hotKeySet.applyDiff(instanceId, entry.getAddedKeys(), entry.getRemovedKeys(), entry.getVersion());
                } else {
                    updated = hotKeySet.update(instanceId, entry.getHotKeys(), entry.getVersion());
                }
                if (updated && log.isDebugEnabled()) {
                    int count = entry.getHotKeys() != null ? entry.getHotKeys().size() : 0;
                    log.debug("Updated hot keys to version {}, count: {}", entry.getVersion(), count);
                }
            });
        }
    }
}
