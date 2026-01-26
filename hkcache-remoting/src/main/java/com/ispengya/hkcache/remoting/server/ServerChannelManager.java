package com.ispengya.hkcache.remoting.server;

import com.ispengya.hkcache.remoting.protocol.Command;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerChannelManager {

    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final Map<String, Set<Channel>> instanceChannels = new ConcurrentHashMap<>();

    private final Map<Channel, String> channelIps = new ConcurrentHashMap<>();

    public void register(Channel channel) {
        channels.add(channel);
    }

    public void unregister(Channel channel) {
        channels.remove(channel);
        channelIps.remove(channel);
        for (Set<Channel> set : instanceChannels.values()) {
            set.remove(channel);
        }
    }

    public ChannelGroup getChannels() {
        return channels;
    }

    public void bindInstance(String instanceId, Channel channel) {
        if (instanceId == null || instanceId.isEmpty() || channel == null) {
            return;
        }
        instanceChannels
                .computeIfAbsent(instanceId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(channel);
        String ip = extractIp(channel);
        if (ip != null) {
            channelIps.put(channel, ip);
        }
    }

    public void broadcastToInstance(String instanceId, Command command) {
        if (instanceId == null || command == null) {
            return;
        }
        Set<Channel> set = instanceChannels.get(instanceId);
        if (set == null) {
            return;
        }
        for (Channel channel : set) {
            if (channel.isActive()) {
                channel.writeAndFlush(command);
            }
        }
    }

    public String getIp(Channel channel) {
        return channelIps.get(channel);
    }

    private String extractIp(Channel channel) {
        SocketAddress remote = channel.remoteAddress();
        if (remote instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) remote;
            if (inet.getAddress() != null) {
                return inet.getAddress().getHostAddress();
            }
            return inet.getHostString();
        }
        return null;
    }
}
