package org.tbox.base.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.tbox.base.core.config.properties.TboxBaseProperties;
import org.tbox.base.core.context.ApplicationContextHolder;
import org.tbox.base.core.exception.handler.DefaultGlobalExceptionHandler;

/**
 * TBox基础组件自动配置类
 * <p>
 * 该类负责自动配置TBox框架的基础组件
 */
@AutoConfiguration
@EnableConfigurationProperties(TboxBaseProperties.class)
@Import({JacksonConfig.class, WebMvcConfig.class,GlobalCorsConfig.class})
public class BaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultGlobalExceptionHandler defaultGlobalExceptionHandler() {
        return new DefaultGlobalExceptionHandler();
    }
}
