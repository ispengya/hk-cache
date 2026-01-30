package com.ispengya.hotkey.cli.detect;

import com.ispengya.hkcache.remoting.client.HotKeyRemotingClient;
import com.ispengya.hkcache.remoting.message.AccessReportMessage;
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
 * HotKeyDetector 负责采集访问轨迹和上报访问统计，
 * 并委托 HotKeyViewRefresher 同步最新的热 Key 视图。
 *
 * <p>该类不直接感知底层通信协议，聚焦在采样和上报行为。</p>
 *
 * @author ispengya
 */
public class HotKeyDetector {

    private static final Logger log = LoggerFactory.getLogger(HotKeyDetector.class);

    private final ScheduledExecutorService scheduler;
    private final HotKeyTransport transport;
    private final HotKeyViewRefresher viewRefresher;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, LongAdder>> accessBuffer = new ConcurrentHashMap<>();

    // 配置参数（后续可抽取到 Properties）
    private final long reportPeriodMillis = 5000L;
    private final long queryPeriodMillis = 30000L;
    private final long queryTimeoutMillis = 3000L;

    /**
     * 构造 HotKeyDetector。
     *
     * @param remotingClient 底层 Remoting 客户端
     * @param hotKeySet      本地热 Key 视图
     */
    public HotKeyDetector(HotKeyRemotingClient remotingClient,
                          HotKeySet hotKeySet) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.transport = new HotKeyTransport(remotingClient);
        this.viewRefresher = new HotKeyViewRefresher(
                transport,
                hotKeySet,
                queryPeriodMillis,
                queryTimeoutMillis
        );
    }

    /**
     * 启动探测器，包括定时上报任务和视图刷新任务。
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
                this::reportTask,
                reportPeriodMillis,
                reportPeriodMillis,
                TimeUnit.MILLISECONDS
        );
        viewRefresher.start();
    }

    /**
     * 停止所有定时任务。
     */
    public void stop() {
        scheduler.shutdown();
        viewRefresher.stop();
    }

    /**
     * 记录一次访问事件，累加到内存缓冲中。
     *
     * @param instanceName 实例名称
     * @param key          业务 Key
     */
    public void recordAccess(String instanceName, String key) {
        if (instanceName == null || key == null) {
            return;
        }
        accessBuffer
                .computeIfAbsent(instanceName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new LongAdder())
                .increment();
    }

    /**
     * 定时执行访问统计上报任务。
     */
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
                transport.reportAccess(message);
            });

        } catch (Exception e) {
            log.error("Failed to report access data", e);
        }
    }
}
