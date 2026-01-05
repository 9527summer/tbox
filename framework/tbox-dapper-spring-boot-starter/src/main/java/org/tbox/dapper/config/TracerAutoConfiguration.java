package org.tbox.dapper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.tbox.dapper.concurrent.TracingThreadPoolAutoConfiguration;
import org.tbox.dapper.mq.TracingMqAutoConfiguration;
import org.tbox.dapper.scheduler.TracingSchedulerAutoConfiguration;
import org.tbox.dapper.web.TraceResponseHeaderInterceptor;
import org.tbox.dapper.web.aspect.WebTraceAspect;

import jakarta.annotation.PostConstruct;

/**
 * TBox-Tracer自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        TracingThreadPoolAutoConfiguration.class,
        TracingSchedulerAutoConfiguration.class,
        TracingMqAutoConfiguration.class
})
public class TracerAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TracerAutoConfiguration.class);

    private final TracerProperties properties;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public TracerAutoConfiguration(TracerProperties properties) {
        this.properties = properties;
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
     * 将 traceId/spanId 写入响应头（traceId/spanId），便于排查。
     * <p>不负责传播（传播由 Spring Boot Tracing / W3C TraceContext 负责）。</p>
     */
    @Bean
    @ConditionalOnClass(name = "io.micrometer.tracing.Tracer")
    @ConditionalOnBean(type = "io.micrometer.tracing.Tracer")
    public WebMvcConfigurer traceResponseHeaderWebMvcConfigurer(io.micrometer.tracing.Tracer tracer) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new TraceResponseHeaderInterceptor(tracer))
                        .addPathPatterns("/**")
                        .order(Ordered.HIGHEST_PRECEDENCE + 10);
            }
        };
    }
} 
