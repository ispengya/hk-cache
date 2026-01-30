package com.ispengya.hotkey.cli.detect;

import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HotKeyViewRefresher 负责通过主动查询与服务端推送两种方式刷新本地热 Key 视图。
 *
 * <p>该类只关注与 Server 的视图同步，不参与访问采集和上报。</p>
 */
public class HotKeyViewRefresher {

    private static final Logger log = LoggerFactory.getLogger(HotKeyViewRefresher.class);

    private final HotKeyTransport transport;
    private final HotKeySet hotKeySet;
    private final ScheduledExecutorService scheduler;

    private final long queryPeriodMillis;
    private final long queryTimeoutMillis;

    /**
     * 构造视图刷新组件。
     *
     * @param transport        与 Server 通信的封装
     * @param hotKeySet        本地热 Key 视图
     * @param queryPeriodMillis 定时查询周期（毫秒）
     * @param queryTimeoutMillis 查询超时时间（毫秒）
     */
    public HotKeyViewRefresher(HotKeyTransport transport,
                               HotKeySet hotKeySet,
                               long queryPeriodMillis,
                               long queryTimeoutMillis) {
        this.transport = transport;
        this.hotKeySet = hotKeySet;
        this.queryPeriodMillis = queryPeriodMillis;
        this.queryTimeoutMillis = queryTimeoutMillis;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.transport.setPushListener(this::handlePush);
    }

    /**
     * 启动视图刷新逻辑，包括定时查询任务。
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
                this::queryTask,
                queryPeriodMillis,
                queryPeriodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 停止视图刷新任务。
     */
    public void stop() {
        scheduler.shutdown();
    }

    private void queryTask() {
        try {
            Map<String, Long> versions = hotKeySet.snapshotVersions();
            HotKeyViewMessage message = transport.queryAllHotKeys(versions, queryTimeoutMillis);
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
        if (views == null || views.isEmpty()) {
            return;
        }
        views.forEach((instanceId, entry) -> {
            if (entry == null) {
                return;
            }
            boolean updated;
            if (entry.getAddedKeys() != null || entry.getRemovedKeys() != null) {
                updated = hotKeySet.applyDiff(
                        instanceId,
                        entry.getAddedKeys(),
                        entry.getRemovedKeys(),
                        entry.getVersion()
                );
            } else {
                updated = hotKeySet.update(
                        instanceId,
                        entry.getHotKeys(),
                        entry.getVersion()
                );
            }
            if (updated && log.isDebugEnabled()) {
                int count = entry.getHotKeys() != null ? entry.getHotKeys().size() : 0;
                log.debug("Updated hot keys to version {}, count: {}", entry.getVersion(), count);
            }
        });
    }
}

