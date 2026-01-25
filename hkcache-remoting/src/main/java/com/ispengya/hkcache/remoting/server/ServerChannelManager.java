package com.ispengya.hkcache.remoting.server;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * ServerChannelManager 负责管理服务端已接入的 Channel 集合。
 *
 * <p>内部基于 Netty 的 {@link ChannelGroup} 实现统一的连接管理，支持统一
 * 广播、关闭等操作。</p>
 */
public class ServerChannelManager {

    /**
     * 当前已注册的所有客户端连接。
     */
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 注册新的客户端连接。
     *
     * @param channel 新建连接对应的 Channel
     */
    public void register(Channel channel) {
        channels.add(channel);
    }

    /**
     * 取消注册客户端连接。
     *
     * @param channel 断开的 Channel
     */
    public void unregister(Channel channel) {
        channels.remove(channel);
    }

    /**
     * 获取当前管理的 Channel 集合。
     *
     * @return ChannelGroup
     */
    public ChannelGroup getChannels() {
        return channels;
    }
}
