package io.jinvoke.rpc.model;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;

public class Message {

    @JSONType(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    public enum Type {
        CALL,
        FORWARD,
        RESPONSE,
        REGISTER,
        HEART_BEAT;

        public static Type fromOrdinal(int ordinal) {
            return switch (ordinal) {
                case 0 -> CALL;
                case 1 -> FORWARD;
                case 2 -> RESPONSE;
                case 3 -> REGISTER;
                case 4 -> HEART_BEAT;
                default -> throw new IllegalArgumentException("Unknown type: " + ordinal);
            };
        }
    }

    public sealed interface Payload permits Request, Response {
    }
}
