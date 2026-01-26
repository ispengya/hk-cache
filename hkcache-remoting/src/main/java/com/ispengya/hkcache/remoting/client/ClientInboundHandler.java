package com.ispengya.hkcache.remoting.client;

import com.ispengya.hkcache.remoting.protocol.Command;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * ClientInboundHandler 是客户端入站处理器。
 *
 * <p>负责在收到服务端响应时，完成挂在 Channel 上的 {@link CompletableFuture}。</p>
 */
public class ClientInboundHandler extends SimpleChannelInboundHandler<Command> {

    /**
     * Channel AttributeKey，用于保存等待响应的 CompletableFuture。
     */
    public static final AttributeKey<CompletableFuture<Command>> FUTURE_KEY =
            AttributeKey.valueOf("future");

    private static volatile Consumer<Command> pushHandler;

    public static void setPushHandler(Consumer<Command> handler) {
        pushHandler = handler;
    }

    /**
     * 收到服务端返回的 Command 时，将其写入对应的 future。
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command msg) {
        CompletableFuture<Command> future = ctx.channel().attr(FUTURE_KEY).get();
        if (future != null) {
            future.complete(msg);
            ctx.channel().attr(FUTURE_KEY).set(null);
            return;
        }
        Consumer<Command> handler = pushHandler;
        if (handler != null) {
            handler.accept(msg);
        }
    }

    /**
     * 收到异常时，将异常传递给 future，并关闭连接。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        CompletableFuture<Command> future = ctx.channel().attr(FUTURE_KEY).get();
        if (future != null) {
            future.completeExceptionally(cause);
            ctx.channel().attr(FUTURE_KEY).set(null);
        }
        ctx.close();
    }
}
