package org.tbox.dapper.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskDecorator;
import org.tbox.dapper.context.TraceContext;

/**
 * 线程池追踪任务装饰器
 * 负责在任务执行时传递追踪上下文到异步线程
 */
public class TracingTaskDecorator implements TaskDecorator {

    private static final Logger log = LoggerFactory.getLogger(TracingTaskDecorator.class);

    public TracingTaskDecorator() {
        // 不需要任何参数
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        if (runnable == null) {
            return null;
        }

        // 获取当前线程的追踪上下文
        TraceContext currentContext = TraceContext.getCurrentContext();
        return () -> {

            try {
                // 恢复捕获的追踪上下文
                if (currentContext != null) {
                    TraceContext.setCurrentContext(currentContext);
                }

                // 执行原始任务
                runnable.run();
            } finally {
                TraceContext.removeContext();
            }
        };
    }
} 