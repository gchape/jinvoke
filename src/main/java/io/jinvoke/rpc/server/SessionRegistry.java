package io.jinvoke.rpc.server;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking active client sessions and pending requests
 */
public final class SessionRegistry {
    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

    // clientId -> client channel
    private static final Map<String, Channel> CLIENTS = new ConcurrentHashMap<>();

    // requestId -> origin client channel (who initiated the request)
    private static final Map<String, Channel> REQUEST_ORIGINS = new ConcurrentHashMap<>();

    private SessionRegistry() {
    }

    public static void registerClient(String clientId, Channel channel) {
        CLIENTS.put(clientId, channel);
        channel.closeFuture().addListener(f -> unregisterClient(clientId));
        log.debug("Client registered: {}", clientId);
    }

    public static void unregisterClient(String clientId) {
        CLIENTS.remove(clientId);
        log.debug("Client unregistered: {}", clientId);
    }

    public static Channel getClient(String clientId) {
        return CLIENTS.get(clientId);
    }

    public static boolean isClientActive(String clientId) {
        Channel ch = CLIENTS.get(clientId);
        return ch != null && ch.isActive();
    }

    public static void trackRequest(String requestId, Channel originClient) {
        REQUEST_ORIGINS.put(requestId, originClient);
        log.debug("Tracking request: {}", requestId);
    }

    public static Channel getOriginClient(String requestId) {
        return REQUEST_ORIGINS.get(requestId);
    }

    public static void removeRequest(String requestId) {
        REQUEST_ORIGINS.remove(requestId);
        log.debug("Removed request: {}", requestId);
    }

    public static int clientCount() {
        return CLIENTS.size();
    }

    public static int pendingRequestCount() {
        return REQUEST_ORIGINS.size();
    }

    public static void clear() {
        REQUEST_ORIGINS.clear();
        CLIENTS.clear();
        log.info("Registry cleared");
    }
}
