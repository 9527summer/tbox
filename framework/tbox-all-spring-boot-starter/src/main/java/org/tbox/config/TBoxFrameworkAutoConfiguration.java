package org.tbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.tbox.base.core.config.BaseAutoConfiguration;
import org.tbox.base.lock.config.LockAutoConfiguration;
import org.tbox.dapper.concurrent.TracingThreadPoolAutoConfiguration;
import org.tbox.dapper.config.TracerAutoConfiguration;
import org.tbox.dapper.mq.TracingMQAutoConfiguration;
import org.tbox.dapper.scheduler.spring.SpringSchedulerAutoConfiguration;
import org.tbox.dapper.scheduler.xxljob.XxlJobTracerAutoConfiguration;
import org.tbox.idempotent.config.IdempotentAutoConfiguration;


/**
 * TBox框架整合自动配置类
 */
@Configuration
@Import({
        BaseAutoConfiguration.class,
        IdempotentAutoConfiguration.class,
        LockAutoConfiguration.class,
        TracerAutoConfiguration.class,
        TracingThreadPoolAutoConfiguration.class,
        TracingMQAutoConfiguration.class,
        XxlJobTracerAutoConfiguration.class,
        SpringSchedulerAutoConfiguration.class

        // 此处可以添加更多模块的自动配置类
})
public class TBoxFrameworkAutoConfiguration {

} 