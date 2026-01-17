package io.jinvoke.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.annotation.Value;

import java.util.logging.Logger;

public class RpcClient {

    private final Logger logger = Logger.getLogger("io.jinvoke.rpc.client");

    @Value("{jinvoke.rpc.client.host}")
    private String host;

    @Value("{jinvoke.rpc.client.port}")
    private String port;

    private Channel channel;

    public RpcClient() {
        Thread.ofPlatform()
                .name("jinvoke-rpc-client")
                .start(this::connect);

        logger.info("Client finished initialization process");
    }

    public void connect() {
        var worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            var client = new Bootstrap()
                    .group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // TODO
                        }

                    });

            client.connect(host, Integer.parseInt(port))
                    .addListener(f -> {
                        if (f.isSuccess()) {
                            this.channel = ((ChannelFuture) f).channel();
                        } else {
                            logger.info("Failed to connect to a server, trying again...");
                            reconnect();
                        }
                    })
                    .channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            worker.shutdownGracefully();
        }
    }

    private void reconnect() {
        // TODO
    }

}
