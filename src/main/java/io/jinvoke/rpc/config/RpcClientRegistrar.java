package io.jinvoke.rpc.config;

import io.jinvoke.rpc.client.RpcClient;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Objects;

public class RpcClientRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attrs = Objects.requireNonNull(
                metadata.getAnnotationAttributes(EnableRpc.class.getName())
        );

        String clientId = (String) attrs.get("clientId");
        String[] basePackages = (String[]) attrs.get("basePackages");

        var beanDef = BeanDefinitionBuilder
                .genericBeanDefinition(RpcClient.class)
                .addPropertyValue("clientId", clientId)
                .addPropertyValue("scanPackages", basePackages)
                .setLazyInit(false)
                .getBeanDefinition();

        registry.registerBeanDefinition("rpcClient", beanDef);
    }
}
