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
    private static final Logger logger = LoggerFactory.getLogger(ServerFrameHandler.class);
    private static final AttributeKey<String> CLIENT_ID = AttributeKey.valueOf("clientId");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame frame) {
        try {
            switch (frame.type()) {
                case INVOKE -> handleInvoke(frame);
                case REGISTER -> handleRegister(ctx, frame);
                case RESULT -> handleResult(frame);
                case HEARTBEAT -> ctx.writeAndFlush(Frame.newHeartbeatFrame(frame.messageId()));
            }
        } catch (Exception e) {
            logger.error("Error handling frame", e);
            ctx.fireExceptionCaught(e);
        }
    }

    private void handleInvoke(Frame frame) {
        InvocationRequest request = frame.asRequest();
        String clientId = request.getClientId();

        if (!SessionRegistry.isClientActive(clientId)) {
            throw new IllegalStateException("Client not active: " + clientId);
        }

        Channel targetClient = SessionRegistry.getClient(clientId);
        SessionRegistry.trackRequest(frame);

        targetClient.writeAndFlush(Frame.newForwardFrame(frame.messageId(), request));
        logger.info("Forwarded request {} to client {}", request.getRequestId(), clientId);
    }

    private void handleRegister(ChannelHandlerContext ctx, Frame frame) {
        InvocationRequest request = frame.asRequest();
        String clientId = request.getClientId();

        ctx.channel().attr(CLIENT_ID).set(clientId);
        SessionRegistry.registerClient(clientId, ctx.channel());

        ctx.writeAndFlush(Frame.newRegisterFrame(frame.messageId()));
    }

    private void handleResult(Frame frame) {
        InvocationResult result = frame.asResult();
        Frame originalRequest = SessionRegistry.getRequest(result.getRequestId());

        if (originalRequest == null) {
            logger.warn("No pending request for: {}", result.getRequestId());
            return;
        }

        Channel originClient = SessionRegistry.getClient(originalRequest.messageId());
        if (originClient != null && originClient.isActive()) {
            originClient.writeAndFlush(frame);
            SessionRegistry.removeRequest(result.getRequestId());
            logger.info("Returned result for request: {}", result.getRequestId());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientId = ctx.channel().attr(CLIENT_ID).get();
        if (clientId != null) {
            SessionRegistry.unregisterClient(clientId);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in handler", cause);
        ctx.close();
    }
}
