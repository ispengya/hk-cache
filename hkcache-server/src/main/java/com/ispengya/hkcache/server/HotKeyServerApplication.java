package com.ispengya.hkcache.server;

import com.ispengya.hkcache.remoting.protocol.CommandType;
import com.ispengya.hkcache.remoting.protocol.JdkSerializer;
import com.ispengya.hkcache.remoting.protocol.Serializer;
import com.ispengya.hkcache.remoting.server.NettyServer;
import com.ispengya.hkcache.remoting.server.NettyServerConfig;
import com.ispengya.hkcache.remoting.server.ServerChannelManager;
import com.ispengya.hkcache.server.core.*;
import com.ispengya.hkcache.server.remoting.DefaultServerRequestDispatcher;
import com.ispengya.hkcache.server.remoting.HotKeyQueryHandler;
import com.ispengya.hkcache.server.remoting.ReportRequestHandler;
import com.ispengya.hkcache.server.scheduler.AggregateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * HotKeyServerApplication 服务端入口程序。
 *
 * <p>组装配置、Netty 组件、核心聚合计算组件，并启动服务。</p>
 *
 * @author ispengya
 */
public class HotKeyServerApplication {

    private static final Logger log = LoggerFactory.getLogger(HotKeyServerApplication.class);

    public static void main(String[] args) {
        log.info("Starting HotKey Server...");

        // 1. Config
        // port=8888, boss=1, worker=4, backlog=1024, timeout=3000, readIdle=60, writeIdle=0, allIdle=0, maxFrame=1MB
        NettyServerConfig serverConfig = new NettyServerConfig(8888, 1, 4, 1024, 3000, 60, 0, 0, 1024 * 1024);

        // 2. Remoting components
        ServerChannelManager channelManager = new ServerChannelManager();
        DefaultServerRequestDispatcher dispatcher = new DefaultServerRequestDispatcher();
        NettyServer nettyServer = new NettyServer(serverConfig, channelManager, dispatcher);

        // 3. Core components
        // Window size 1s, 5 slots = 5s sliding window
        InMemoryHotKeyAggregator aggregator = new InMemoryHotKeyAggregator(1000L, 5);
        HotKeyAggregateService aggregateService = new HotKeyAggregateService(aggregator);
        InMemoryHotKeyResultStore resultStore = new InMemoryHotKeyResultStore();
        
        // Threshold algorithm: key is hot if count >= 3 in the window
        // 3 is a low threshold for testing; in production it might be 100 or more
        ThresholdHotKeyAlgorithm algorithm = new ThresholdHotKeyAlgorithm(3);
        
        Serializer serializer = new JdkSerializer();

        // 4. Register Handlers
        dispatcher.registerHandler(CommandType.ACCESS_REPORT, new ReportRequestHandler(aggregateService, serializer));
        dispatcher.registerHandler(CommandType.HOT_KEY_QUERY, new HotKeyQueryHandler(resultStore, serializer));

        // 5. Scheduler
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ExecutorService workerPool = Executors.newFixedThreadPool(4);
        AggregateScheduler aggregateScheduler = new AggregateScheduler(
                scheduler,
                workerPool,
                aggregateService,
                algorithm,
                resultStore,
                aggregator, // aggregator implements InstanceRegistry
                1000L // Compute every 1s
        );

        // 6. Bootstrap
        HotKeyServerBootstrap bootstrap = new HotKeyServerBootstrap(nettyServer, aggregateScheduler);
        bootstrap.start();

        log.info("HotKey Server started on port {}", serverConfig.getPort());

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping HotKey Server...");
            bootstrap.stop();
            log.info("HotKey Server stopped.");
        }));
    }
}
