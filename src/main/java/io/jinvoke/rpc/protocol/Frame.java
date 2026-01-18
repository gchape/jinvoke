package io.jinvoke.rpc.protocol;

import java.io.Serializable;
import java.util.Objects;

public record Frame(
        String messageId,
        Protocol.MessageType type,
        Protocol.Payload payload
) implements Serializable {

    public Frame {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        if (requiresPayload(type) && payload == null) {
            throw new IllegalArgumentException(
                    "Message type " + type + " requires a payload"
            );
        }
    }

    private static boolean requiresPayload(Protocol.MessageType type) {
        return type == Protocol.MessageType.INVOKE ||
                type == Protocol.MessageType.FORWARD ||
                type == Protocol.MessageType.RESULT;
    }

    public static Frame newInvokeFrame(String messageId, InvocationRequest request) {
        return new Frame(messageId, Protocol.MessageType.INVOKE, request);
    }

    public static Frame newForwardFrame(String messageId, InvocationRequest request) {
        return new Frame(messageId, Protocol.MessageType.FORWARD, request);
    }

    public static Frame newResultFrame(String messageId, InvocationResult result) {
        return new Frame(messageId, Protocol.MessageType.RESULT, result);
    }

    public static Frame newRegisterFrame(String messageId) {
        return new Frame(messageId, Protocol.MessageType.REGISTER, null);
    }

    public static Frame newHeartbeatFrame(String messageId) {
        return new Frame(messageId, Protocol.MessageType.HEARTBEAT, null);
    }

    public boolean hasPayload() {
        return payload != null;
    }

    public boolean isRequest() {
        return type == Protocol.MessageType.INVOKE ||
                type == Protocol.MessageType.FORWARD;
    }

    public boolean isResponse() {
        return type == Protocol.MessageType.RESULT;
    }

    public InvocationRequest asRequest() {
        if (!isRequest()) {
            throw new IllegalStateException("Frame is not a request: " + type);
        }
        return (InvocationRequest) payload;
    }

    public InvocationResult asResult() {
        if (!isResponse()) {
            throw new IllegalStateException("Frame is not a response: " + type);
        }
        return (InvocationResult) payload;
    }
}
