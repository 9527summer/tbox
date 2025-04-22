package org.tbox.base.core.exception.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tbox.base.core.exception.handler.DefaultGlobalExceptionHandler;

@Configuration
public class ExceptionHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(annotation = RestControllerAdvice.class)
    public DefaultGlobalExceptionHandler defaultGlobalExceptionHandler() {
        return new DefaultGlobalExceptionHandler();
    }
}
