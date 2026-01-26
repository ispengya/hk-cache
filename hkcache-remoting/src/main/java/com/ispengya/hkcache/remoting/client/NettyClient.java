package com.ispengya.hkcache.remoting.client;

import com.ispengya.hkcache.remoting.codec.CommandDecoder;
import com.ispengya.hkcache.remoting.codec.CommandEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * NettyClient 封装基于 Netty 的客户端启动与连接逻辑。
 *
 * <p>客户端采用短连接模式：每次请求创建一次连接，用完即关。</p>
 */
public final class NettyClient {

    /**
     * 客户端配置。
     */
    private final NettyClientConfig config;

    /**
     * worker 事件循环组。
     */
    private EventLoopGroup workerGroup;

    /**
     * Netty Bootstrap 实例。
     */
    private Bootstrap bootstrap;

    /**
     * 长连接 Channel。
     */
    private volatile Channel channel;

    /**
     * 构造 NettyClient。
     *
     * @param config 客户端配置
     */
    public NettyClient(NettyClientConfig config) {
        this.config = config;
    }

    /**
     * 初始化 Netty 客户端，包括 EventLoopGroup 和 ChannelPipeline。
     */
    public void start() {
        workerGroup = new NioEventLoopGroup(config.getWorkerThreads());
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new CommandDecoder(config.getMaxFrameLength()));
                        p.addLast(new CommandEncoder());
                        p.addLast(new ClientInboundHandler());
                    }
                });
    }

    /**
     * 获取或建立长连接 Channel。
     *
     * @return 已连接的 Channel
     * @throws InterruptedException 连接被中断时抛出
     */
    public Channel getOrCreateChannel() throws InterruptedException {
        Channel current = channel;
        if (current != null && current.isActive()) {
            return current;
        }
        synchronized (this) {
            current = channel;
            if (current != null && current.isActive()) {
                return current;
            }
            InetSocketAddress address = selectServerAddress();
            ChannelFuture future = bootstrap.connect(address).sync();
            channel = future.channel();
            return channel;
        }
    }

    /**
     * 从配置中选择一个 server 地址。
     *
     * @return 选中的 server 地址
     */
    private InetSocketAddress selectServerAddress() {
        if (config.getServerAddresses() == null || config.getServerAddresses().isEmpty()) {
            throw new IllegalStateException("No server addresses configured");
        }
        // 简单实现：返回列表中的第一个地址，可按需扩展成轮询或随机。
        return config.getServerAddresses().get(0);
    }

    /**
     * 停止客户端并释放资源。
     */
    public void stop() {
        Channel current = channel;
        if (current != null) {
            current.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
