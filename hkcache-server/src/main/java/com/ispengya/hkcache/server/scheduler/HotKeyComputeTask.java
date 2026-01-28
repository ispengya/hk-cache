package com.ispengya.hkcache.server.scheduler;

import com.ispengya.hkcache.remoting.message.HotKeyViewMessage;
import com.ispengya.hkcache.remoting.protocol.Command;
import com.ispengya.hkcache.remoting.protocol.CommandType;
import com.ispengya.hkcache.remoting.protocol.Serializer;
import com.ispengya.hkcache.remoting.server.ServerChannelManager;
import com.ispengya.hkcache.server.core.AggregatedKeyStat;
import com.ispengya.hkcache.server.core.HotKeyAggregateService;
import com.ispengya.hkcache.server.core.HotKeyComputeAlgorithm;
import com.ispengya.hkcache.server.core.HotKeyResultStore;
import com.ispengya.hkcache.server.model.HotKeyResult;

import java.util.Set;

public final class HotKeyComputeTask implements Runnable {

    private final String instanceId;
    private final HotKeyAggregateService aggregateService;
    private final HotKeyComputeAlgorithm algorithm;
    private final HotKeyResultStore resultStore;
    private final ServerChannelManager channelManager;
    private final Serializer serializer;

    public HotKeyComputeTask(String instanceId,
                             HotKeyAggregateService aggregateService,
                             HotKeyComputeAlgorithm algorithm,
                             HotKeyResultStore resultStore,
                             ServerChannelManager channelManager,
                             Serializer serializer) {
        this.instanceId = instanceId;
        this.aggregateService = aggregateService;
        this.algorithm = algorithm;
        this.resultStore = resultStore;
        this.channelManager = channelManager;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        Iterable<AggregatedKeyStat> stats = aggregateService.snapshot(instanceId);
        Set<String> hotKeys = algorithm.computeHotKeys(stats);
        HotKeyResult result = HotKeyResult.from(instanceId, hotKeys);
        resultStore.update(result);

        HotKeyViewMessage view = new HotKeyViewMessage();
        view.setInstanceId(result.getInstanceId());
        view.setVersion(result.getVersion());
        view.setHotKeys(result.getHotKeys());
        byte[] payload = serializer.serialize(view);
        Command command = new Command(CommandType.HOT_KEY_PUSH, 0L, payload);
        channelManager.broadcastToInstance(instanceId, command);
    }
}
