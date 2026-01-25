package com.ispengya.hkcache.remoting.client;

import com.ispengya.hkcache.remoting.protocol.Command;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.util.concurrent.CompletableFuture;

/**
 * ClientRequestSender 封装客户端请求发送逻辑。
 *
 * <p>对上层暴露简化的 One-Way 和 Sync 两种发送方式。</p>
 */
public final class ClientRequestSender {

    /**
     * 底层 NettyClient，用于建立连接。
     */
    private final NettyClient nettyClient;

    /**
     * 构造请求发送器。
     *
     * @param nettyClient Netty 客户端
     */
    public ClientRequestSender(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    /**
     * 发送单向请求，不关心响应结果。
     *
     * @param command 待发送的命令
     */
    public void sendOneWay(Command command) {
        Channel channel = null;
        try {
            channel = nettyClient.connectOnce();
            channel.writeAndFlush(command).addListener(f -> {
                if (!f.isSuccess()) {
                    // 这里可以按需记录日志或埋点
                }
                // 写入完成后关闭连接，实现短连接语义
                if (f instanceof ChannelFuture) {
                    ((ChannelFuture) f).channel().close();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 发送同步请求，返回一个可等待的 Future。
     *
     * @param command      待发送的命令
     * @param timeoutMillis 超时时间（上层负责处理超时）
     * @return 包含响应 Command 的 CompletableFuture
     */
    public CompletableFuture<Command> sendSync(Command command, long timeoutMillis) {
        CompletableFuture<Command> future = new CompletableFuture<>();
        Channel channel = null;
        try {
            channel = nettyClient.connectOnce();
            // 将 future 挂到 Channel 上，供 ClientInboundHandler 回传结果
            channel.attr(ClientInboundHandler.FUTURE_KEY).set(future);

            channel.writeAndFlush(command);

            // 收到响应或异常后关闭连接
            Channel finalChannel = channel;
            future.whenComplete((resp, ex) -> finalChannel.close());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}
