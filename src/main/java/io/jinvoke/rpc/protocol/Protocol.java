package io.jinvoke.rpc.protocol;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;

import java.io.Serializable;

public final class Protocol {
    private Protocol() {
    }

    @JSONType(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    public enum MessageType {
        INVOKE,
        FORWARD,
        RESULT,
        REGISTER,
        HEARTBEAT;

        public static MessageType fromByte(byte b) {
            if (b < 0 || b >= values().length) {
                throw new IllegalArgumentException("Invalid message type: " + b);
            }
            return values()[b];
        }

        public byte toByte() {
            return (byte) ordinal();
        }
    }

    public sealed interface Payload extends Serializable
            permits InvocationRequest, InvocationResult, Registration {
    }
}
