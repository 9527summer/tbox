package org.tbox.dapper.scheduler.xxljob;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbox.dapper.config.TracerProperties;

/**
 * XXL-Job追踪自动配置
 * 当项目中存在XXL-Job依赖时自动添加追踪功能
 */

@Configuration
@ConditionalOnClass(XxlJob.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobTracerAutoConfiguration {

    /**
     * 注册XXL-Job任务追踪切面
     * @return XXL-Job任务追踪切面
     */
    @Bean
    public XxlJobTraceAspect xxlJobTraceAspect(TracerProperties tracerProperties) {
        return new XxlJobTraceAspect(tracerProperties);
    }
} 