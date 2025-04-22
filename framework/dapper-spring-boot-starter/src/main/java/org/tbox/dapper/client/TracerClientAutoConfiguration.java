package org.tbox.dapper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.tbox.dapper.config.TracerProperties;
import org.tbox.dapper.core.TracerMetricsCollector;

/**
 * HTTP客户端追踪的自动配置类
 */
@Configuration
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracerClientAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TracerClientAutoConfiguration.class);

    /**
     * 配置RestTemplate拦截器
     */
    @Configuration
    @ConditionalOnClass(RestTemplate.class)
    public static class RestTemplateConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public TracerRestTemplateInterceptor tracerRestTemplateInterceptor(
                TracerProperties properties, TracerMetricsCollector metricsCollector) {
            log.debug("创建TracerRestTemplateInterceptor bean");
            return new TracerRestTemplateInterceptor(properties, metricsCollector);
        }
    }
    
    /**
     * 配置OkHttp拦截器
     */
    @Configuration
    @ConditionalOnClass(name = "okhttp3.OkHttpClient")
    public static class OkHttpConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public TracerOkHttpInterceptor tracerOkHttpInterceptor(
                TracerProperties properties, TracerMetricsCollector metricsCollector) {
            log.debug("创建TracerOkHttpInterceptor bean");
            return new TracerOkHttpInterceptor(properties, metricsCollector);
        }
    }
    
    /**
     * 配置Apache HttpClient拦截器
     */
    @Configuration
    @ConditionalOnClass(name = "org.apache.http.client.HttpClient")
    public static class HttpClientConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public TracerHttpClientInterceptor tracerHttpClientInterceptor(
                TracerProperties properties, TracerMetricsCollector metricsCollector) {
            log.debug("创建TracerHttpClientInterceptor bean");
            return new TracerHttpClientInterceptor(properties, metricsCollector);
        }
    }
} 