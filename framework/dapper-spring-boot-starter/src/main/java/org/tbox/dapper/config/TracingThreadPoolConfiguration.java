package org.tbox.dapper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.tbox.dapper.concurrent.TracingTaskDecorator;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 追踪线程池配置类
 * 用于提供支持追踪上下文传递的线程池组件
 */
@Configuration
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingThreadPoolConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TracingThreadPoolConfiguration.class);

    private final TracerProperties tracerProperties;

    public TracingThreadPoolConfiguration(TracerProperties tracerProperties) {
        this.tracerProperties = tracerProperties;
        log.debug("初始化追踪线程池配置");
    }

    /**
     * 提供追踪任务装饰器
     * 用于在异步任务执行时传递追踪上下文
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskDecorator tracingTaskDecorator() {
        log.debug("创建追踪任务装饰器");
        return new TracingTaskDecorator();
    }
    
    /**
     * 已移除默认的tracingTaskExecutor
     * 应用应该使用自己的TaskExecutor配置或使用TracingTaskDecorator装饰现有执行器
     * 如需配置，请自行创建TaskExecutor并使用tracingTaskDecorator进行包装
     */
    /*
    @Bean("tracingTaskExecutor")
    @ConditionalOnMissingBean(name = "tracingTaskExecutor")
    public Executor tracingTaskExecutor(TaskDecorator tracingTaskDecorator) {
        // 使用默认的线程池配置
        int coreSize = 8;
        int maxSize = 32;
        int queueCapacity = 1000;
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("tracer-async-");
        executor.setTaskDecorator(tracingTaskDecorator);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        log.info("创建追踪任务执行器: coreSize={}, maxSize={}, queueCapacity={}",
                coreSize, maxSize, queueCapacity);
        
        return executor;
    }
    */
}