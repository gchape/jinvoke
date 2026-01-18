package io.jinvoke.rpc.client;

import io.jinvoke.rpc.codec.FrameDecoder;
import io.jinvoke.rpc.codec.FrameEncoder;
import io.jinvoke.rpc.config.Rpc;
import io.jinvoke.rpc.model.RpcMethodDescriptor;
import io.jinvoke.rpc.protocol.Frame;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RpcClient implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    private final Map<String, RpcMethodDescriptor> methods = new ConcurrentHashMap<>();

    @Value("${jinvoke.rpc.client.host:localhost}")
    private String host;

    @Value("${jinvoke.rpc.client.port:8080}")
    private int port;

    @Value("${jinvoke.rpc.client.id}")
    private String clientId;

    @Value("${jinvoke.rpc.scan.packages:}")
    private String[] scanPackages;

    private Channel channel;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void initialize() {
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        Thread.ofVirtual().start(this::connect);
        logger.info("RPC Client initialized");
    }

    private void connect() {
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new FrameDecoder())
                                    .addLast(new FrameEncoder());
                            //.addLast(new ClientFrameHandler());
                        }
                    });

            channel = bootstrap.connect(host, port).sync().channel();
            logger.info("Connected to {}:{}", host, port);

            channel.writeAndFlush(Frame.newRegisterFrame(clientId));

            channel.closeFuture().sync();
            logger.warn("Disconnected from server");
        } catch (Exception e) {
            logger.error("Connection failed: {}", e.getMessage());
            reconnect();
        }
    }

    private void reconnect() {
        try {
            TimeUnit.SECONDS.sleep(5);
            Thread.ofVirtual().start(this::connect);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (scanPackages == null) return;

        for (String pkg : scanPackages) {
            var scanner = new ClassPathScanningCandidateComponentProvider(false) {{
                addIncludeFilter(new AssignableTypeFilter(Object.class));
            }};

            scanner.findCandidateComponents(pkg).forEach(bd -> {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    Arrays.stream(clazz.getDeclaredMethods())
                            .filter(method -> method.isAnnotationPresent(Rpc.class))
                            .forEach(method -> {
                                var descriptor = RpcMethodDescriptor.from(method);
                                methods.put(descriptor.getMethodId(), descriptor);
                            });
                } catch (Exception e) {
                    logger.error("Failed to scan class", e);
                }
            });
        }

        logger.info("Registered {} RPC methods", methods.size());
    }

    public Channel getChannel() {
        return channel;
    }

    public String getClientId() {
        return clientId;
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) channel.close();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        logger.info("RPC Client shutdown");
    }
}
