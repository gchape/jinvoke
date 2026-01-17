package io.jinvoke.rpc.model;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;

import java.io.Serializable;

public class Message {

    @JSONType(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    public enum Type {
        CALL,
        FORWARD,
        RESPONSE,
        REGISTER,
        HEART_BEAT,
    }

    public sealed interface Payload permits Request, Response {
    }

    public record Packet(String id, Type type, Payload payload) implements Serializable {
    }
}
