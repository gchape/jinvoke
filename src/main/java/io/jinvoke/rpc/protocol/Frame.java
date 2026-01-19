package io.jinvoke.rpc.protocol;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record Frame(
        String messageId,
        Protocol.MessageType type,
        Protocol.Payload payload
) implements Serializable {

    public Frame {
        Objects.requireNonNull(messageId, "messageId required");
        Objects.requireNonNull(type, "type required");
        validatePayload(type, payload);
    }

    private static void validatePayload(Protocol.MessageType type, Protocol.Payload payload) {
        boolean needsPayload = switch (type) {
            case INVOKE, FORWARD, RESULT, REGISTER -> true;
            case HEARTBEAT -> false;
        };

        if (needsPayload && payload == null) {
            throw new IllegalArgumentException(type + " requires payload");
        }
        if (!needsPayload && payload != null) {
            throw new IllegalArgumentException(type + " should not have payload");
        }
    }

    public static Frame invoke(InvocationRequest request) {
        return new Frame(request.requestId(), Protocol.MessageType.INVOKE, request);
    }

    public static Frame forward(String messageId, InvocationRequest request) {
        return new Frame(messageId, Protocol.MessageType.FORWARD, request);
    }

    public static Frame result(InvocationResult result) {
        return new Frame(result.requestId(), Protocol.MessageType.RESULT, result);
    }

    public static Frame register(String clientId) {
        return new Frame(newId(), Protocol.MessageType.REGISTER, new Registration(clientId));
    }

    public static Frame heartbeat() {
        return new Frame(newId(), Protocol.MessageType.HEARTBEAT, null);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    public boolean hasPayload() {
        return payload != null;
    }

    public InvocationRequest asRequest() {
        if (!(payload instanceof InvocationRequest req)) {
            throw new IllegalStateException("Not a request frame: " + type);
        }
        return req;
    }

    public InvocationResult asResult() {
        if (!(payload instanceof InvocationResult res)) {
            throw new IllegalStateException("Not a result frame: " + type);
        }
        return res;
    }

    public Registration asRegistration() {
        if (!(payload instanceof Registration reg)) {
            throw new IllegalStateException("Not a registration frame: " + type);
        }
        return reg;
    }
}
