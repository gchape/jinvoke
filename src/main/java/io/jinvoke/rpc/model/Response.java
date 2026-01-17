package io.jinvoke.rpc.model;

import java.io.Serializable;

public record Response(String id, Object result) implements Message.Payload, Serializable {
}
