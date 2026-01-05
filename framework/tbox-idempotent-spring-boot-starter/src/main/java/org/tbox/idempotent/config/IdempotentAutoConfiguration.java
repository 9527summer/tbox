package org.tbox.idempotent.config;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.tbox.idempotent.core.IdempotentAspect;
import org.tbox.idempotent.core.IdempotentExecuteHandlerFactory;
import org.tbox.idempotent.core.UserIdProvider;
import org.tbox.idempotent.core.param.IdempotentParamExecuteHandler;
import org.tbox.idempotent.core.param.IdempotentParamService;
import org.tbox.idempotent.core.spel.IdempotentSpELExecuteHandler;
import org.tbox.idempotent.core.spel.IdempotentSpelService;

/**
 * 幂等自动装配
 */
@AutoConfiguration
@EnableConfigurationProperties(IdempotentProperties.class)
@Import(UserIdProviderConfiguration.class)
public class IdempotentAutoConfiguration {

    /**
     * 幂等切面
     */
    @Bean
    public IdempotentAspect idempotentAspect(IdempotentExecuteHandlerFactory idempotentExecuteHandlerFactory) {
        return new IdempotentAspect(idempotentExecuteHandlerFactory);
    }

    @Bean
    public IdempotentExecuteHandlerFactory idempotentExecuteHandlerFactory(IdempotentParamService idempotentParamService, IdempotentSpelService idempotentSpelService) {
        return new IdempotentExecuteHandlerFactory(idempotentParamService, idempotentSpelService);
    }

    /**
     * 参数方式幂等实现，基于 RestAPI 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentParamService idempotentParamExecuteHandler(IdempotentProperties idempotentProperties, UserIdProvider userIdProvider) {
        return new IdempotentParamExecuteHandler(idempotentProperties, userIdProvider);
    }

    /**
     * SpEL表达式方式幂等实现，只根据表达式提取的参数值做幂等校验
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentSpelService idempotentSpelExecuteHandler(IdempotentProperties idempotentProperties) {
        return new IdempotentSpELExecuteHandler(idempotentProperties);
    }
}
