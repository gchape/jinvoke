package io.jinvoke.rpc.model;

import java.io.Serializable;

public record Packet(
        String id,
        Message.Type type,
        Message.Payload payload
) implements Serializable {
}
