package io.jinvoke.rpc.server;

import io.jinvoke.rpc.protocol.Frame;
import io.jinvoke.rpc.protocol.InvocationRequest;
import io.jinvoke.rpc.protocol.InvocationResult;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerFrameHandler extends SimpleChannelInboundHandler<Frame> {
    private static final Logger log = LoggerFactory.getLogger(ServerFrameHandler.class);
    private static final AttributeKey<String> CLIENT_ID = AttributeKey.valueOf("clientId");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame frame) {
        try {
            switch (frame.type()) {
                case INVOKE -> handleInvoke(ctx, frame);
                case REGISTER -> handleRegister(ctx, frame);
                case RESULT -> handleResult(frame);
                case HEARTBEAT -> ctx.writeAndFlush(Frame.heartbeat());
                default -> log.warn("Unexpected frame: {}", frame.type());
            }
        } catch (Exception e) {
            log.error("Error handling frame", e);
            ctx.fireExceptionCaught(e);
        }
    }

    private void handleInvoke(ChannelHandlerContext ctx, Frame frame) {
        InvocationRequest request = frame.asRequest();
        String targetClientId = request.clientId();

        Channel targetClient = SessionRegistry.getClient(targetClientId);
        if (targetClient == null || !targetClient.isActive()) {
            InvocationResult error = InvocationResult.failure(
                    request.requestId(),
                    new IllegalStateException("Client not available: " + targetClientId)
            );
            ctx.writeAndFlush(Frame.result(error));
            return;
        }

        // Track request origin
        SessionRegistry.trackRequest(request.requestId(), ctx.channel());

        // Forward to target client
        targetClient.writeAndFlush(Frame.forward(frame.messageId(), request));
        log.info("Forwarded {} to client {}", request.requestId(), targetClientId);
    }

    private void handleRegister(ChannelHandlerContext ctx, Frame frame) {
        String clientId = frame.asRegistration().clientId();

        ctx.channel().attr(CLIENT_ID).set(clientId);
        SessionRegistry.registerClient(clientId, ctx.channel());

        ctx.writeAndFlush(Frame.register(clientId));
        log.info("Client registered: {}", clientId);
    }

    private void handleResult(Frame frame) {
        InvocationResult result = frame.asResult();
        Channel originClient = SessionRegistry.getOriginClient(result.requestId());

        if (originClient == null || !originClient.isActive()) {
            log.warn("Origin client not found for: {}", result.requestId());
            return;
        }

        originClient.writeAndFlush(frame);
        SessionRegistry.removeRequest(result.requestId());

        log.info("Returned result for: {}", result.requestId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientId = ctx.channel().attr(CLIENT_ID).get();
        if (clientId != null) {
            SessionRegistry.unregisterClient(clientId);
            log.info("Client disconnected: {}", clientId);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in handler", cause);
        ctx.close();
    }
}
