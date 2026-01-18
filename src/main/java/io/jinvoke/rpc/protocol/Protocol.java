package io.jinvoke.rpc.protocol;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;

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

        public static MessageType fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= values().length) {
                throw new IllegalArgumentException("Invalid message type ordinal: " + ordinal);
            }
            return values()[ordinal];
        }

        public byte toOrdinal() {
            return (byte) ordinal();
        }
    }

    public sealed interface Payload permits InvocationRequest, InvocationResult {
    }
}
