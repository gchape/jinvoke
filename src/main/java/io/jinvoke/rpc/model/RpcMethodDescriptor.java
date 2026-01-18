package io.jinvoke.rpc.model;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;

public class RpcMethodDescriptor {
    private final String methodId;

    private final String className;
    private final String methodName;

    private final String[] paramNames;
    private final int parameterCount;
    private final String[] paramTypes;

    private final String returnType;
    private final boolean hasReturnValue;

    private RpcMethodDescriptor(Builder builder) {
        this.methodId = builder.methodId;

        this.className = builder.className;
        this.methodName = builder.methodName;

        this.paramNames = builder.paramNames;
        this.parameterCount = builder.parameterCount;
        this.paramTypes = builder.paramTypes;

        this.returnType = builder.returnType;
        this.hasReturnValue = builder.hasReturnValue;
    }

    public static RpcMethodDescriptor from(Method method) {
        Objects.requireNonNull(method, "method cannot be null");

        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();

        String[] paramNames = Arrays.stream(method.getParameters())
                .map(Parameter::getName)
                .toArray(String[]::new);

        String[] paramTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .toArray(String[]::new);

        String returnType = method.getReturnType().getName();

        boolean hasReturnValue = !returnType.equals("void");

        String methodId = generateMethodId(className, methodName, paramTypes);

        return new Builder()
                .methodId(methodId)
                .className(className)
                .methodName(methodName)
                .paramNames(paramNames)
                .parameterCount(paramNames.length)
                .paramTypes(paramTypes)
                .returnType(returnType)
                .hasReturnValue(hasReturnValue)
                .build();
    }

    private static String generateMethodId(String className, String methodName, String[] paramTypes) {
        return className + "#" + methodName + "(" + String.join(",", paramTypes) + ")";
    }

    public String getMethodId() {
        return methodId;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParamNames() {
        return paramNames.clone();
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public String[] getParamTypes() {
        return paramTypes.clone();
    }

    public String getReturnType() {
        return returnType;
    }

    public boolean hasReturnValue() {
        return hasReturnValue;
    }

    public String getSignature() {
        return methodName + "(" + String.join(", ", paramTypes) + "): " + returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcMethodDescriptor that = (RpcMethodDescriptor) o;
        return Objects.equals(methodId, that.methodId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodId);
    }

    @Override
    public String toString() {
        return "RpcMethodDescriptor{" +
                "methodId='" + methodId + '\'' +
                ", signature='" + getSignature() + '\'' +
                '}';
    }

    public static class Builder {
        private String methodId;
        private String className;
        private String methodName;
        private String[] paramNames;
        private int parameterCount;
        private String[] paramTypes;
        private String returnType;
        private boolean hasReturnValue;

        public Builder methodId(String methodId) {
            this.methodId = methodId;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder paramNames(String[] paramNames) {
            this.paramNames = paramNames;
            return this;
        }

        public Builder parameterCount(int parameterCount) {
            this.parameterCount = parameterCount;
            return this;
        }

        public Builder paramTypes(String[] paramTypes) {
            this.paramTypes = paramTypes;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder hasReturnValue(boolean hasReturnValue) {
            this.hasReturnValue = hasReturnValue;
            return this;
        }

        public RpcMethodDescriptor build() {
            Objects.requireNonNull(className, "className required");
            Objects.requireNonNull(methodName, "methodName required");
            Objects.requireNonNull(returnType, "returnType required");

            return new RpcMethodDescriptor(this);
        }
    }
}