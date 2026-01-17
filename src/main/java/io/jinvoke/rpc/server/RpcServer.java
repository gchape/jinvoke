package io.jinvoke.rpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RpcServer {

    static void main() {
        new RpcServer().start();
    }

    public void start() {
        // Boss group accepts incoming connections; worker group handles I/O for established channels
        var boss = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        var worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            var server = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // TODO
                        }
                    });

            var fut = server.bind(8888);
            // Block the current thread until the server channel is closed
            fut.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // Gracefully shut down event loop groups to release threads and resources
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}