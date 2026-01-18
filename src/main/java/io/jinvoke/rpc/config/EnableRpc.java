package io.jinvoke.rpc.config;

import io.jinvoke.rpc.client.RpcClient;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RpcClient.class)
public @interface EnableRpc {
    String clientId() default "";

    String[] basePackages() default {};
}
