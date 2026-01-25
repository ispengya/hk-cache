package com.ispengya.hkcache.server.remoting;

import com.ispengya.hkcache.remoting.message.AccessReportMessage;
import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.Serializer;
import com.ispengya.hkcache.server.core.HotKeyAggregateService;
import com.ispengya.hkcache.server.model.AccessReport;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ReportRequestHandler 处理访问统计上报请求。
 *
 * <p>对应命令类型：{@link com.ispengya.hkcache.remoting.protocol.CommandType#ACCESS_REPORT}。
 * 解析消息体并将访问记录录入聚合服务。</p>
 *
 * @author ispengya
 */
public final class ReportRequestHandler implements RequestHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportRequestHandler.class);

    private final HotKeyAggregateService aggregateService;
    private final Serializer serializer;

    /**
     * 构造上报请求处理器。
     *
     * @param aggregateService 聚合服务
     * @param serializer       序列化器
     */
    public ReportRequestHandler(HotKeyAggregateService aggregateService,
                                Serializer serializer) {
        this.aggregateService = aggregateService;
        this.serializer = serializer;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, Command command) {
        try {
            AccessReportMessage message = serializer.deserialize(command.getPayload(), AccessReportMessage.class);
            if (message == null) {
                return;
            }

            Map<String, Integer> counts = message.getKeyAccessCounts();
            if (counts != null) {
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String key = entry.getKey();
                    Integer count = entry.getValue();
                    if (count == null || count <= 0) {
                        continue;
                    }
                    AccessReport report = new AccessReport(
                            message.getInstanceId(),
                            key,
                            message.getTimestamp(),
                            true, // 假设成功，因为 CLI 仅上报计数
                            0L,   // CLI 实现中暂未上报 RT
                            count
                    );
                    aggregateService.record(report);
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle access report", e);
        }
    }
}
