package io.jinvoke.rpc.server;

import io.jinvoke.rpc.protocol.Frame;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SessionRegistry.class);

    private static final Map<String, Channel> CLIENTS = new ConcurrentHashMap<>();
    private static final Map<String, Frame> PENDING_REQUESTS = new ConcurrentHashMap<>();

    private SessionRegistry() {
    }

    public static void registerClient(String clientId, Channel channel) {
        CLIENTS.put(clientId, channel);
        channel.closeFuture().addListener(f -> unregisterClient(clientId));
        logger.info("Client registered: {}", clientId);
    }

    public static void unregisterClient(String clientId) {
        CLIENTS.remove(clientId);
        logger.info("Client unregistered: {}", clientId);
    }

    public static Channel getClient(String clientId) {
        return CLIENTS.get(clientId);
    }

    public static boolean isClientActive(String clientId) {
        Channel ch = CLIENTS.get(clientId);
        return ch != null && ch.isActive();
    }

    public static void trackRequest(Frame frame) {
        String requestId = frame.asRequest().getRequestId();
        PENDING_REQUESTS.put(requestId, frame);
        logger.debug("Tracking request: {}", requestId);
    }

    public static Frame getRequest(String requestId) {
        return PENDING_REQUESTS.get(requestId);
    }

    public static void removeRequest(String requestId) {
        PENDING_REQUESTS.remove(requestId);
    }

    public static void clear() {
        PENDING_REQUESTS.clear();
        CLIENTS.clear();
    }
}
