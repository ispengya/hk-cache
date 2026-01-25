package com.ispengya.hkcache.remoting.client;

import com.ispengya.hkcache.remoting.message.AccessReportMessage;
import com.ispengya.hkcache.remoting.message.HotKeyQueryRequest;
import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.CommandType;
import com.ispengya.hkcache.remoting.protocol.Serializer;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HotKeyRemotingClient 封装了与服务端交互的业务请求逻辑。
 *
 * @author ispengya
 */
public final class HotKeyRemotingClient {

    private final Serializer serializer;
    private final ClientRequestSender sender;

    public HotKeyRemotingClient(Serializer serializer,
                                ClientRequestSender sender) {
        this.serializer = serializer;
        this.sender = sender;
    }

    /**
     * 上报访问统计数据 (OneWay)
     */
    public void reportAccess(AccessReportMessage message) {
        byte[] bytes = serializer.serialize(message);
        Command command = new Command(CommandType.ACCESS_REPORT, bytes);
        sender.sendOneWay(command);
    }

    /**
     * 查询热 Key 视图 (Sync)
     */
    public HotKeyViewMessage queryHotKeys(String instanceId, long lastVersion, long timeoutMillis) {
        HotKeyQueryRequest request = new HotKeyQueryRequest(instanceId, lastVersion);
        byte[] bytes = serializer.serialize(request);
        Command command = new Command(CommandType.HOT_KEY_QUERY, bytes);
        try {
            CompletableFuture<Command> future = sender.sendSync(command, timeoutMillis);
            Command responseCommand = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return serializer.deserialize(responseCommand.getPayload(), HotKeyViewMessage.class);
        } catch (Exception e) {
            // 发生异常（超时或网络错误）时返回空结果或 fallback
            // 这里返回一个空视图，避免上层 NPE，业务层可以根据 version 判断是否更新
            return new HotKeyViewMessage(instanceId, lastVersion, Collections.emptySet());
        }
    }
}
