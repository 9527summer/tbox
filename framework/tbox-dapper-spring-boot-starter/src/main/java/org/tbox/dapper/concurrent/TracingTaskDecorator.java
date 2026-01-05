package org.tbox.dapper.concurrent;

import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

/**
 * 基于 Spring Boot 3 / Spring Framework 的上下文传播任务装饰器。
 *
 * <p>基于 Spring 6 的 {@link ContextPropagatingTaskDecorator}（内部基于 Micrometer Context Propagation），
 * 用于传播 Observation/Tracing/MDC 等上下文。</p>
 */
public class TracingTaskDecorator implements TaskDecorator {

    private final TaskDecorator delegate = new ContextPropagatingTaskDecorator();

    public TracingTaskDecorator() {
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        return delegate.decorate(runnable);
    }
}
