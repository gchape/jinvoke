package io.jinvoke.rpc.client;

import io.jinvoke.rpc.protocol.Frame;
import io.jinvoke.rpc.protocol.InvocationRequest;
import io.jinvoke.rpc.protocol.InvocationResult;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class ClientFrameHandler extends SimpleChannelInboundHandler<Frame> {
    private static final Logger log = LoggerFactory.getLogger(ClientFrameHandler.class);

    private final RpcClient client;

    public ClientFrameHandler(RpcClient client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Frame frame) {
        try {
            switch (frame.type()) {
                case FORWARD -> handleForward(ctx, frame);
                case RESULT -> handleResult(frame);
                case HEARTBEAT -> ctx.writeAndFlush(Frame.heartbeat());
                default -> log.warn("Unexpected frame type: {}", frame.type());
            }
        } catch (Exception e) {
            log.error("Error handling frame", e);
        }
    }

    private void handleForward(ChannelHandlerContext ctx, Frame frame) {
        InvocationRequest request = frame.asRequest();

        try {
            Object result = invokeLocal(request);
            ctx.writeAndFlush(Frame.result(InvocationResult.success(request.requestId(), result)));
            log.info("Executed: {}", request.signature());
        } catch (Exception e) {
            ctx.writeAndFlush(Frame.result(InvocationResult.failure(request.requestId(), e)));
            log.error("Execution failed: {}", request.signature(), e);
        }
    }

    private Object invokeLocal(InvocationRequest request) throws Exception {
        var descriptor = client.methods().get(
                methodId(request.targetClass(), request.targetMethod(), request.paramTypes())
        );

        if (descriptor == null) {
            throw new NoSuchMethodException("Method not found: " + request.signature());
        }

        Class<?> clazz = Class.forName(request.targetClass());
        Method method = clazz.getMethod(request.targetMethod(),
                toClasses(request.paramTypes()));

        Object instance = clazz.getDeclaredConstructor().newInstance();
        return method.invoke(instance, request.params());
    }

    private void handleResult(Frame frame) {
        InvocationResult result = frame.asResult();
        CompletableFuture<Object> future =
                (CompletableFuture<Object>) client.removeRequest(result.requestId());

        if (future == null) {
            log.warn("No pending request for: {}", result.requestId());
            return;
        }

        if (result.isSuccess()) {
            future.complete(((InvocationResult.Success) result).value());
        } else {
            future.completeExceptionally(((InvocationResult.Failure) result).error());
        }
    }

    private String methodId(String className, String methodName, String[] paramTypes) {
        return className + "#" + methodName + "(" + String.join(",", paramTypes) + ")";
    }

    private Class<?>[] toClasses(String[] typeNames) throws ClassNotFoundException {
        Class<?>[] classes = new Class<?>[typeNames.length];
        for (int i = 0; i < typeNames.length; i++) {
            classes[i] = Class.forName(typeNames[i]);
        }
        return classes;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in handler", cause);
        ctx.close();
    }
}
