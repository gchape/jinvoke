package io.jinvoke.rpc.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record Registration(String clientId) implements Protocol.Payload, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Registration {
        Objects.requireNonNull(clientId, "clientId required");
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be blank");
        }
    }
}
