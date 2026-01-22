package io.jinvoke.rpc.client;

import io.jinvoke.rpc.protocol.Frame;
import io.jinvoke.rpc.protocol.InvocationRequest;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RpcProxyFactory {

    private final RpcClient client;

    public RpcProxyFactory(RpcClient client) {
        this.client = client;
    }

    public <T> T generate(Class<T> clazz, String requestClientId) {
        return generate(clazz, requestClientId, 30, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public <T> T generate(Class<T> clazz, String requestClientId, long timeout, TimeUnit unit) {
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{clazz},
                (_, method, args) -> {
                    // Check connection
                    if (client.channel() == null || !client.channel().isActive()) {
                        throw new IllegalStateException("RPC client not connected");
                    }

                    String[] paramTypes = Arrays.stream(method.getParameterTypes())
                            .map(Class::getName)
                            .toArray(String[]::new);

                    var request = InvocationRequest.of(
                            requestClientId,
                            clazz.getName(),
                            method.getName(),
                            args,
                            paramTypes,
                            method.getReturnType().getName());

                    if (method.getReturnType() == void.class) {
                        client.channel().writeAndFlush(Frame.invoke(request));
                        return null;
                    }

                    CompletableFuture<Object> future = new CompletableFuture<>();
                    client.trackRequest(request.requestId(), future);

                    client.channel().writeAndFlush(Frame.invoke(request))
                            .addListener(f -> {
                                if (!f.isSuccess()) {
                                    client.removeRequest(request.requestId());
                                    future.completeExceptionally(f.cause());
                                }
                            });

                    try {
                        return future.get(timeout, unit);
                    } catch (TimeoutException e) {
                        client.removeRequest(request.requestId());
                        throw new RuntimeException("Request timed out", e);
                    } catch (ExecutionException e) {
                        throw e.getCause();
                    }
                });
    }
}
