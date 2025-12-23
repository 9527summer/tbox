package org.tbox.dapper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.tbox.dapper.client.TracerClientAutoConfiguration;
import org.tbox.dapper.web.TracerWebInterceptor;
import org.tbox.dapper.web.aspect.WebTraceAspect;

import javax.annotation.PostConstruct;

/**
 * TBox-Tracer自动配置
 */
@Configuration
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(TracerClientAutoConfiguration.class)
public class TracerAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TracerAutoConfiguration.class);

    @Autowired
    private TracerProperties properties;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public TracerAutoConfiguration() {
        log.debug("Initializing TBox-Tracer");
    }

    /**
     * 应用启动时自动设置应用名称
     */
    @PostConstruct
    public void init() {
        if (properties.getApplicationName() == null) {
            properties.setApplicationName(applicationName);
            log.debug("Set tracer application name to: {}", applicationName);
        }
    }

    /**
     * 注册接口出入参日志记录切面
     */
    @Bean
    @ConditionalOnProperty(prefix = "tbox.tracer", name = "printPayload", havingValue = "true", matchIfMissing = true)
    public WebTraceAspect webTraceAspect() {
        log.debug("Registering web trace aspect for request/response logging");
        return new WebTraceAspect();
    }

    /**
     * 注册Web配置，添加拦截器
     */
    @Bean
    public WebMvcConfigurer tracerWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                log.debug("Registering tracer web interceptor");

                TracerWebInterceptor interceptor = new TracerWebInterceptor(properties);
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/**")
                        .order(Ordered.HIGHEST_PRECEDENCE + 10);
            }
        };
    }



} 