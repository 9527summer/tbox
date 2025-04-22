package org.tbox.dapper.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.tbox.dapper.client.TracerClientAutoConfiguration;
import org.tbox.dapper.core.TracerMetricsCollector;
import org.tbox.dapper.web.TracerFilter;
import org.tbox.dapper.web.TracerWebInterceptor;
import org.tbox.dapper.web.aspect.WebTraceAspect;

import javax.annotation.PostConstruct;

/**
 * TBox-Tracer自动配置
 */
@Configuration
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
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
     * 注册指标收集器
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    public TracerMetricsCollector tracerMetricsCollector(MeterRegistry meterRegistry) {
        log.debug("Registering tracer metrics collector");
        return new TracerMetricsCollector(meterRegistry, properties);
    }

    /**
     * 注册跟踪过滤器
     */
    @Bean
    public FilterRegistrationBean<TracerFilter> tracerFilterRegistration() {
        log.debug("Registering tracer filter");
        
        FilterRegistrationBean<TracerFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TracerFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("tracerFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        return registration;
    }

    /**
     * 注册Web配置，添加拦截器
     */
    @Bean
    @ConditionalOnBean(TracerMetricsCollector.class)
    public WebMvcConfigurer tracerWebMvcConfigurer(TracerMetricsCollector metricsCollector) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                log.debug("Registering tracer web interceptor");
                
                TracerWebInterceptor interceptor = new TracerWebInterceptor(properties, metricsCollector);
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/**")
                        .order(Ordered.HIGHEST_PRECEDENCE + 10);
            }
        };
    }

    /**
     * 注册默认指标收集器（当没有MeterRegistry时使用）
     */
    @Bean
    @ConditionalOnMissingBean({MeterRegistry.class, TracerMetricsCollector.class})
    public TracerMetricsCollector defaultTracerMetricsCollector() {
        log.warn("No MeterRegistry found, using no-op metrics collector");
        
        // 创建一个无操作的指标收集器
        return new NoOpTracerMetricsCollector(properties);
    }
    
    /**
     * 无操作的指标收集器实现
     * 当应用中不存在MeterRegistry时使用
     */
    private static class NoOpTracerMetricsCollector extends TracerMetricsCollector {
        
        public NoOpTracerMetricsCollector(TracerProperties properties) {
            super(null, properties);
        }
        
        @Override
        public void recordRequestStart(String method, String uri, String controller, String action) {
            // 无操作实现
        }
        
        @Override
        public void recordRequestEnd(String method, String uri, String controller, String action,
                                     int statusCode, long duration, boolean hasException) {
            // 无操作实现
        }
        
        @Override
        public void recordRequestSize(long bytes) {
            // 无操作实现
        }
        
        @Override
        public void recordResponseSize(long bytes) {
            // 无操作实现
        }
    }
} 