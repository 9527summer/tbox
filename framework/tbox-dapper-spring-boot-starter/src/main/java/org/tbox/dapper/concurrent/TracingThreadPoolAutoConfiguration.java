package org.tbox.dapper.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池追踪自动配置类
 * 只要检测到相关类存在，就自动注册追踪功能，无需用户配置
 */
@Configuration
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
public class TracingThreadPoolAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(TracingThreadPoolAutoConfiguration.class);
    
    @Bean
    public TracingTaskDecorator tracingTaskDecorator() {
        return new TracingTaskDecorator();
    }
    
    @Bean
    public TracingThreadPoolBeanPostProcessor tracingThreadPoolBeanPostProcessor(TaskDecorator tracingTaskDecorator) {
        return new TracingThreadPoolBeanPostProcessor(tracingTaskDecorator);
    }
} 