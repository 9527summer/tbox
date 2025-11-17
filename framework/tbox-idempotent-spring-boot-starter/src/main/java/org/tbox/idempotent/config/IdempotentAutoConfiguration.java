package org.tbox.idempotent.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.tbox.base.lock.service.LockService;
import org.tbox.idempotent.core.IdempotentAspect;
import org.tbox.idempotent.core.UserIdProvider;
import org.tbox.idempotent.core.param.IdempotentParamExecuteHandler;
import org.tbox.idempotent.core.param.IdempotentParamService;

/**
 * 幂等自动装配
 */
@EnableConfigurationProperties(IdempotentProperties.class)
@Import(UserIdProviderConfiguration.class)
public class IdempotentAutoConfiguration {

    /**
     * 幂等切面
     */
    @Bean
    public IdempotentAspect idempotentAspect() {
        return new IdempotentAspect();
    }

    /**
     * 参数方式幂等实现，基于 RestAPI 场景
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotentParamService idempotentParamExecuteHandler(LockService lockService, IdempotentProperties idempotentProperties, UserIdProvider userIdProvider) {
        return new IdempotentParamExecuteHandler(lockService,idempotentProperties,userIdProvider);
    }
}
