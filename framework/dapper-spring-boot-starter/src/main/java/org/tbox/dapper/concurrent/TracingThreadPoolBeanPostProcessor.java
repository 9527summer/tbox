package org.tbox.dapper.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Field;

/**
 * 线程池追踪Bean后处理器
 * 负责自动为Spring容器中的线程池添加追踪功能
 */
public class TracingThreadPoolBeanPostProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(TracingThreadPoolBeanPostProcessor.class);
    
    private final TaskDecorator tracingTaskDecorator;
    
    public TracingThreadPoolBeanPostProcessor(TaskDecorator tracingTaskDecorator) {
        this.tracingTaskDecorator = tracingTaskDecorator;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
            
            // 使用反射获取原有的装饰器（如果有）
            TaskDecorator existingDecorator = getExistingTaskDecorator(executor);
            
            if (existingDecorator == null) {
                // 如果没有现有装饰器，直接设置追踪装饰器
                executor.setTaskDecorator(tracingTaskDecorator);

            } else if (existingDecorator != tracingTaskDecorator) {
                // 如果已经有装饰器但不是追踪装饰器，创建复合装饰器
                executor.setTaskDecorator(task -> {
                    // 先应用已有装饰器
                    Runnable decoratedByExisting = existingDecorator.decorate(task);
                    // 再应用追踪装饰器
                    return tracingTaskDecorator.decorate(decoratedByExisting);
                });
            }
        }
        
        return bean;
    }
    
    /**
     * 使用反射获取ThreadPoolTaskExecutor已配置的TaskDecorator
     * 由于某些版本的Spring中ThreadPoolTaskExecutor没有提供getTaskDecorator()方法，所以使用反射
     */
    private TaskDecorator getExistingTaskDecorator(ThreadPoolTaskExecutor executor) {
        try {
            Field taskDecoratorField = ThreadPoolTaskExecutor.class.getDeclaredField("taskDecorator");
            taskDecoratorField.setAccessible(true);
            return (TaskDecorator) taskDecoratorField.get(executor);
        } catch (NoSuchFieldException e) {
            log.warn("无法通过反射获取ThreadPoolTaskExecutor的taskDecorator字段，可能是Spring版本不同: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("反射异常详情", e);
            }
            return null;
        } catch (IllegalAccessException e) {
            log.warn("无法访问ThreadPoolTaskExecutor的taskDecorator字段: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("反射访问异常详情", e);
            }
            return null;
        }
    }
} 