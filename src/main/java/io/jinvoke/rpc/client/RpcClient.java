package io.jinvoke.rpc.client;

import io.jinvoke.rpc.codec.FrameCodec;
import io.jinvoke.rpc.config.Rpc;
import io.jinvoke.rpc.model.RpcMethodDescriptor;
import io.jinvoke.rpc.protocol.Frame;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RpcClient implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

    private final Map<String, RpcMethodDescriptor> methods = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<?>> pendingRequests = new ConcurrentHashMap<>();

    @Value("${jinvoke.rpc.client.host:localhost}")
    private String host;

    @Value("${jinvoke.rpc.client.port:8080}")
    private int port;

    private String clientId;
    private String[] scanPackages;

    private Channel channel;
    private EventLoopGroup workerGroup;

    @Override
    public void afterSingletonsInstantiated() {
        scanRpcMethods();
        startAsync();
    }

    private void startAsync() {
        CompletableFuture.runAsync(this::connect)
                .exceptionally(ex -> {
                    log.error("Failed to start RPC client", ex);
                    return null;
                });
    }

    private void connect() {
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new FrameCodec())
                                    .addLast(new ClientFrameHandler(RpcClient.this));
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            log.info("Connected to {}:{}", host, port);

            channel.writeAndFlush(Frame.register(clientId))
                    .addListener(f -> {
                        if (f.isSuccess()) {
                            log.info("Client registered: {}", clientId);
                        }
                    });

            channel.closeFuture().sync();
            log.warn("Disconnected from server");
        } catch (Exception e) {
            log.error("Connection failed: {}", e.getMessage());
            reconnect();
        }
    }

    private void reconnect() {
        try {
            TimeUnit.SECONDS.sleep(5);
            connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void scanRpcMethods() {
        if (scanPackages == null) return;

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        for (String pkg : scanPackages) {
            scanner.findCandidateComponents(pkg).forEach(bd -> {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    Arrays.stream(clazz.getDeclaredMethods())
                            .filter(m -> m.isAnnotationPresent(Rpc.class))
                            .forEach(m -> {
                                var desc = RpcMethodDescriptor.from(m);
                                methods.put(desc.methodId(), desc);
                            });
                } catch (Exception e) {
                    log.error("Failed to scan class: {}", bd.getBeanClassName(), e);
                }
            });
        }

        log.info("Registered {} RPC methods", methods.size());
    }

    public Channel channel() {
        return channel;
    }

    public String clientId() {
        return clientId;
    }

    public Map<String, RpcMethodDescriptor> methods() {
        return Map.copyOf(methods);
    }

    public void trackRequest(String requestId, CompletableFuture<?> future) {
        pendingRequests.put(requestId, future);
    }

    public CompletableFuture<?> removeRequest(String requestId) {
        return pendingRequests.remove(requestId);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) channel.close();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("RPC Client shutdown");
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setScanPackages(String[] scanPackages) {
        this.scanPackages = scanPackages;
    }
}
