package com.ispengya.hkcache.server.scheduler;

import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.CommandType;
import com.ispengya.hkcache.remoting.protocol.Serializer;
import com.ispengya.hkcache.remoting.server.ServerChannelManager;
import com.ispengya.hkcache.server.model.HotKeyResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HotKeyChangePublisher {

    private final ServerChannelManager channelManager;
    private final Serializer serializer;

    public HotKeyChangePublisher(ServerChannelManager channelManager, Serializer serializer) {
        this.channelManager = channelManager;
        this.serializer = serializer;
    }

    public void publish(HotKeyResult result, Set<String> addedKeys, Set<String> removedKeys) {
        if ((addedKeys == null || addedKeys.isEmpty()) && (removedKeys == null || removedKeys.isEmpty())) {
            return;
        }
        HotKeyViewMessage view = new HotKeyViewMessage();
        view.setInstanceId(result.getInstanceId());
        view.setVersion(result.getVersion());
        HotKeyViewMessage.ViewEntry entry = new HotKeyViewMessage.ViewEntry();
        entry.setVersion(result.getVersion());
        entry.setHotKeys(result.getHotKeys());
        entry.setAddedKeys(addedKeys);
        entry.setRemovedKeys(removedKeys);
        Map<String, HotKeyViewMessage.ViewEntry> views = new HashMap<>();
        views.put(result.getInstanceId(), entry);
        view.setViews(views);
        byte[] payload = serializer.serialize(view);
        Command command = new Command(CommandType.HOT_KEY_PUSH, 0L, payload);
        channelManager.broadcastOnPushChannels(command);
    }
}

