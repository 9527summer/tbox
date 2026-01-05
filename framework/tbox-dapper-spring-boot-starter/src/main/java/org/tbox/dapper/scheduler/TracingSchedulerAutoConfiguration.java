package org.tbox.dapper.scheduler;

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.tbox.dapper.config.TracerProperties;
import org.tbox.dapper.scheduler.spring.ScheduledTaskTraceAspect;
import org.tbox.dapper.scheduler.xxljob.XxlJobTraceAspect;

@AutoConfiguration
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingSchedulerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TracingSchedulerAutoConfiguration.class);

    @ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
    public static class XxlJobTracerAutoConfiguration {
        /**
         * 注册XXL-Job任务追踪切面
         * @return XXL-Job任务追踪切面
         */
        @Bean
        public XxlJobTraceAspect xxlJobTraceAspect(TracerProperties tracerProperties, ObservationRegistry observationRegistry) {
            log.debug("自动注册xxlJob追踪拦截器");
            return new XxlJobTraceAspect(tracerProperties, observationRegistry);
        }
    }


    public static class SpringSchedulerAutoConfiguration {

        /**
         * 注册Spring @Scheduled任务追踪切面
         * @return 任务追踪切面
         */
        @Bean
        public ScheduledTaskTraceAspect scheduledTaskTraceAspect(TracerProperties tracerProperties, ObservationRegistry observationRegistry) {
            log.debug("自动注册scheduled追踪拦截器");
            return new ScheduledTaskTraceAspect(tracerProperties, observationRegistry);
        }
    }





}
