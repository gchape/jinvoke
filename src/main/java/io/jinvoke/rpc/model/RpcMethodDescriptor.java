package io.jinvoke.rpc.model;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public record RpcMethodDescriptor(
        String methodId,
        String className,
        String methodName,
        String[] paramTypes,
        String returnType
) {
    public RpcMethodDescriptor {
        Objects.requireNonNull(className, "className required");
        Objects.requireNonNull(methodName, "methodName required");
        Objects.requireNonNull(returnType, "returnType required");
        paramTypes = paramTypes == null ? new String[0] : paramTypes.clone();
        methodId = generateMethodId(className, methodName, paramTypes);
    }

    public static RpcMethodDescriptor from(Method method) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        String[] paramTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .toArray(String[]::new);
        String returnType = method.getReturnType().getName();

        return new RpcMethodDescriptor(null, className, methodName, paramTypes, returnType);
    }

    private static String generateMethodId(String className, String methodName, String[] paramTypes) {
        return className + "#" + methodName + "(" + String.join(",", paramTypes) + ")";
    }

    @Override
    public String[] paramTypes() {
        return paramTypes.clone();
    }

    public boolean hasReturnValue() {
        return !"void".equals(returnType);
    }

    public String signature() {
        return methodName + "(" + String.join(", ", paramTypes) + "): " + returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RpcMethodDescriptor that)) return false;
        return Objects.equals(methodId, that.methodId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodId);
    }
}
