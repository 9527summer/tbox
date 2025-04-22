package org.tbox.dapper.scheduler.spring;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbox.dapper.config.TracerProperties;


/**
 * Spring调度任务追踪自动配置
 */

@Configuration
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringSchedulerAutoConfiguration {

    /**
     * 注册Spring @Scheduled任务追踪切面
     * @return 任务追踪切面
     */
    @Bean
    public ScheduledTaskTraceAspect scheduledTaskTraceAspect(TracerProperties tracerProperties) {
        return new ScheduledTaskTraceAspect(tracerProperties);
    }
} 