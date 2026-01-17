package io.jinvoke.rpc.model;

import java.io.Serializable;

public final class Request implements Message.Payload, Serializable {
    private final String id;
    private final String clientId;
    private final String className;
    private final String methodName;
    private final Object[] params;
    private final String[] paramTypes;
    private final String returnType;

    private Request(Builder builder) {
        this.id = builder.id;
        this.clientId = builder.clientId;
        this.className = builder.className;
        this.methodName = builder.methodName;
        this.params = builder.params;
        this.paramTypes = builder.paramTypes;
        this.returnType = builder.returnType;
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getParams() {
        return params;
    }

    public String[] getParamTypes() {
        return paramTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public static class Builder {
        private String id;
        private String clientId;
        private String className;
        private String methodName;
        private Object[] params;
        private String[] paramTypes;
        private String returnType;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
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

        public Builder params(Object[] params) {
            this.params = params;
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

        public Request build() {
            return new Request(this);
        }
    }
}
