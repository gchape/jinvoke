package io.jinvoke.rpc.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RpcClientRegistrar.class)
public @interface EnableRpc {
    String clientId() default "";

    String[] basePackages() default {};
}
