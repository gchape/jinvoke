package io.jinvoke.rpc.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public sealed interface InvocationResult extends Protocol.Payload, Serializable
        permits InvocationResult.Success, InvocationResult.Failure {

    static Success success(String requestId, Object value) {
        return new Success(requestId, value);
    }

    static Failure failure(String requestId, Throwable error) {
        return new Failure(requestId, error);
    }

    String requestId();

    boolean isSuccess();

    record Success(String requestId, Object value) implements InvocationResult {
        @Serial
        private static final long serialVersionUID = 1L;

        public Success {
            Objects.requireNonNull(requestId, "requestId required");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        public boolean hasValue() {
            return value != null;
        }
    }

    record Failure(String requestId, Throwable error) implements InvocationResult {
        @Serial
        private static final long serialVersionUID = 1L;

        public Failure {
            Objects.requireNonNull(requestId, "requestId required");
            Objects.requireNonNull(error, "error required");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        public String errorType() {
            return error.getClass().getName();
        }

        public String errorMessage() {
            return error.getMessage();
        }
    }
}
