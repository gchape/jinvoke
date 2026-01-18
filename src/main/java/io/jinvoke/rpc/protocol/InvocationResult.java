package io.jinvoke.rpc.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class InvocationResult implements Protocol.Payload, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Object value;
    private final String requestId;
    private final Throwable error;

    private InvocationResult(String requestId, Object value, Throwable error) {
        this.requestId = Objects.requireNonNull(requestId, "requestId cannot be null");
        this.value = value;
        this.error = error;
    }

    public static InvocationResult success(String requestId, Object value) {
        return new InvocationResult(requestId, value, null);
    }

    public static InvocationResult failure(String requestId, Throwable error) {
        Objects.requireNonNull(error, "error cannot be null for failure result");
        return new InvocationResult(requestId, null, error);
    }

    public String getRequestId() {
        return requestId;
    }

    public Object getValue() {
        return value;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public boolean hasValue() {
        return value != null;
    }

    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    public String getErrorType() {
        return error != null ? error.getClass().getName() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvocationResult that = (InvocationResult) o;
        return Objects.equals(requestId, that.requestId) &&
                Objects.equals(value, that.value) &&
                Objects.equals(error != null ? error.getMessage() : null,
                        that.error != null ? that.error.getMessage() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, value, error != null ? error.getMessage() : null);
    }

    @Override
    public String toString() {
        return "InvocationResult{" +
                "requestId='" + requestId + '\'' +
                ", status=" + (isSuccess() ? "SUCCESS" : "FAILURE") +
                (hasValue() ? ", hasValue=true" : "") +
                (error != null ? ", error=" + error.getClass().getSimpleName() +
                        ": " + error.getMessage() : "") +
                '}';
    }
}
