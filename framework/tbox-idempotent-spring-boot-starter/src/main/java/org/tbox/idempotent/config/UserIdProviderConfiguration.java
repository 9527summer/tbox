package org.tbox.idempotent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.tbox.idempotent.core.DefaultUserIdProvider;
import org.tbox.idempotent.core.UserIdProvider;

/**
 * 配置USERID默认配置
 */
@Configuration
public class UserIdProviderConfiguration {


    @Bean
    @ConditionalOnMissingBean(UserIdProvider.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public UserIdProvider defaultUserIdProvider() {
        return new DefaultUserIdProvider();
    }
}
