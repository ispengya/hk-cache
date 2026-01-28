package com.ispengya.hkcache.server.remoting;

import com.ispengya.hkcache.remoting.message.HotKeyQueryRequest;
import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.CommandType;
import com.ispengya.hkcache.remoting.protocol.Serializer;
import com.ispengya.hkcache.server.core.HotKeyResultStore;
import com.ispengya.hkcache.server.model.HotKeyResult;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * HotKeyQueryHandler 处理热 Key 查询请求。
 *
 * <p>对应命令类型：{@link com.ispengya.hkcache.remoting.protocol.CommandType#HOT_KEY_QUERY}。
 * 根据实例 ID 和版本号返回最新的热 Key 视图。</p>
 *
 * @author ispengya
 */
public final class HotKeyQueryHandler implements RequestHandler {

    private static final Logger log = LoggerFactory.getLogger(HotKeyQueryHandler.class);

    private final HotKeyResultStore resultStore;
    private final Serializer serializer;

    /**
     * 构造查询请求处理器。
     *
     * @param resultStore 热 Key 结果存储
     * @param serializer  序列化器
     */
    public HotKeyQueryHandler(HotKeyResultStore resultStore,
                              Serializer serializer) {
        this.resultStore = resultStore;
        this.serializer = serializer;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, Command command) {
        try {
            HotKeyQueryRequest request = serializer.deserialize(command.getPayload(), HotKeyQueryRequest.class);
            if (request == null) {
                return;
            }

            HotKeyResult result = resultStore.get(request.getInstanceId());
            HotKeyViewMessage responseMsg;

            if (result == null) {
                // 无结果，返回空集合
                responseMsg = new HotKeyViewMessage(
                        request.getInstanceId(),
                        0L,
                        Collections.emptySet()
                );
            } else if (result.getVersion() <= request.getLastVersion()) {
                // 版本未变更，目前策略为返回当前结果（客户端可根据版本号判断是否更新）
                // 优化点：若版本一致，可仅返回空集合 + 当前版本，减少带宽。
                // 简化实现：返回全量。
                responseMsg = new HotKeyViewMessage(
                        result.getInstanceId(),
                        result.getVersion(),
                        result.getHotKeys()
                );
            } else {
                // 有新版本
                responseMsg = new HotKeyViewMessage(
                        result.getInstanceId(),
                        result.getVersion(),
                        result.getHotKeys()
                );
            }

            byte[] payload = serializer.serialize(responseMsg);
            Command response = new Command(CommandType.HOT_KEY_QUERY, command.getRequestId(), payload);
            ctx.writeAndFlush(response);

        } catch (Exception e) {
            log.error("Failed to handle hot key query", e);
        }
    }
}
