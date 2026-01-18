package io.jinvoke.rpc.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public final class InvocationRequest implements Protocol.Payload, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String clientId;
    private final String requestId;

    private final String targetClass;
    private final String targetMethod;

    private final Object[] params;
    private final String[] paramTypes;
    private final String returnType;

    private InvocationRequest(Builder builder) {
        this.clientId = Objects.requireNonNull(builder.clientId, "clientId required");
        this.requestId = Objects.requireNonNull(builder.requestId, "requestId required");

        this.targetClass = Objects.requireNonNull(builder.targetClass, "targetClass required");
        this.targetMethod = Objects.requireNonNull(builder.targetMethod, "targetMethod required");

        this.params = builder.arguments != null ? builder.arguments.clone() : new Object[0];
        this.paramTypes = builder.argumentTypes != null ? builder.argumentTypes.clone() : new String[0];

        this.returnType = builder.returnType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public Object[] getParams() {
        return params.clone();
    }

    public String[] getParamTypes() {
        return paramTypes.clone();
    }

    public String getReturnType() {
        return returnType;
    }

    public int getArgumentCount() {
        return params.length;
    }

    public boolean hasReturnValue() {
        return returnType != null && !"void".equals(returnType);
    }

    public boolean hasArguments() {
        return params.length > 0;
    }

    public String getMethodSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(targetClass).append(".").append(targetMethod).append("(");
        if (paramTypes.length > 0) {
            sb.append(String.join(", ", paramTypes));
        }
        sb.append(")");
        if (returnType != null) {
            sb.append(":").append(returnType);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvocationRequest that = (InvocationRequest) o;
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

    @Override
    public String toString() {
        return "InvocationRequest{" +
                "requestId='" + requestId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", method=" + getMethodSignature() +
                ", argumentCount=" + params.length +
                '}';
    }

    public static final class Builder {
        private String requestId;
        private String clientId;
        private String targetClass;
        private String targetMethod;
        private Object[] arguments;
        private String[] argumentTypes;
        private String returnType;

        private Builder() {
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder targetClass(String targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder targetMethod(String targetMethod) {
            this.targetMethod = targetMethod;
            return this;
        }

        public Builder arguments(Object... arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder argumentTypes(String... argumentTypes) {
            this.argumentTypes = argumentTypes;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder returnType(Class<?> returnType) {
            this.returnType = returnType.getName();
            return this;
        }

        public InvocationRequest build() {
            validate();
            return new InvocationRequest(this);
        }

        private void validate() {
            if (requestId == null || requestId.isBlank()) {
                throw new IllegalStateException("requestId is required");
            }
            if (clientId == null || clientId.isBlank()) {
                throw new IllegalStateException("clientId is required");
            }
            if (targetClass == null || targetClass.isBlank()) {
                throw new IllegalStateException("targetClass is required");
            }
            if (targetMethod == null || targetMethod.isBlank()) {
                throw new IllegalStateException("targetMethod is required");
            }
            if (arguments != null && argumentTypes != null &&
                    arguments.length != argumentTypes.length) {
                throw new IllegalStateException(
                        "arguments and argumentTypes must have same length"
                );
            }
        }
    }
}
