package io.jinvoke.rpc.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record InvocationRequest(
        String requestId,
        String clientId,
        String targetClass,
        String targetMethod,
        Object[] params,
        String[] paramTypes,
        String returnType
) implements Protocol.Payload, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvocationRequest {
        Objects.requireNonNull(requestId, "requestId required");
        Objects.requireNonNull(clientId, "clientId required");
        Objects.requireNonNull(targetClass, "targetClass required");
        Objects.requireNonNull(targetMethod, "targetMethod required");

        params = params == null ? new Object[0] : params.clone();
        paramTypes = paramTypes == null ? new String[0] : paramTypes.clone();

        if (params.length != paramTypes.length) {
            throw new IllegalArgumentException("params and paramTypes length mismatch");
        }
    }

    public static InvocationRequest of(String clientId, String targetClass,
                                       String targetMethod, Object[] params, String[] paramTypes, String returnType) {
        return new InvocationRequest(
                UUID.randomUUID().toString(),
                clientId, targetClass, targetMethod,
                params, paramTypes, returnType
        );
    }

    // Defensive copies
    @Override
    public Object[] params() {
        return params.clone();
    }

    @Override
    public String[] paramTypes() {
        return paramTypes.clone();
    }

    public boolean hasReturnValue() {
        return returnType != null && !"void".equals(returnType);
    }

    public String signature() {
        return String.format("%s.%s(%s):%s",
                targetClass, targetMethod,
                String.join(",", paramTypes),
                returnType != null ? returnType : "void");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvocationRequest that)) return false;
        return Objects.equals(requestId, that.requestId) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(targetClass, that.targetClass) &&
                Objects.equals(targetMethod, that.targetMethod) &&
                Arrays.equals(params, that.params) &&
                Arrays.equals(paramTypes, that.paramTypes) &&
                Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(requestId, clientId, targetClass, targetMethod, returnType);
        result = 31 * result + Arrays.hashCode(params);
        result = 31 * result + Arrays.hashCode(paramTypes);
        return result;
    }
}
