package org.tbox.dapper.scheduler.xxljob;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.config.TracerProperties;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.lang.reflect.Method;

/**
 * XXL-Job任务追踪切面
 * 自动拦截所有使用@XxlJob注解的方法，添加追踪上下文
 */
@Aspect
public class XxlJobTraceAspect {
    private static final Logger log = LoggerFactory.getLogger(XxlJobTraceAspect.class);

    private TracerProperties tracerProperties;

    public XxlJobTraceAspect(TracerProperties tracerProperties) {
        this.tracerProperties = tracerProperties;
    }

    /**
     * 拦截所有带有@XxlJob注解的方法
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("@annotation(com.xxl.job.core.handler.annotation.XxlJob)")
    public Object traceXxlJob(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();
        
        // 获取XxlJob注解信息
        XxlJob xxlJob = method.getAnnotation(XxlJob.class);
        String jobName = xxlJob.value();
        
        // 创建追踪上下文
        TraceContext traceContext = TraceContext.createRootContext(tracerProperties.getApplicationName());
        traceContext.setAttribute(TracerConstants.COMPONENT_TYPE, "scheduled-task");
        traceContext.setAttribute(TracerConstants.RESOURCE_TYPE, "xxl-job");
        traceContext.setAttribute(TracerConstants.RESOURCE_NAME, jobName);
        traceContext.setAttribute("xxl.job.class", className);
        traceContext.setAttribute("xxl.job.method", methodName);

        
        // 尝试获取XXL-Job上下文信息
        try {
            long jobId = XxlJobHelper.getJobId();
            String jobParam = XxlJobHelper.getJobParam();
            int shardIndex = XxlJobHelper.getShardIndex();
            int shardTotal = XxlJobHelper.getShardTotal();
            
            traceContext.setAttribute("xxl.job.id", String.valueOf(jobId));
            if (jobParam != null && !jobParam.isEmpty()) {
                traceContext.setAttribute("xxl.job.param", jobParam);
            }
            if (shardTotal > 1) {
                traceContext.setAttribute("xxl.job.shard.index", String.valueOf(shardIndex));
                traceContext.setAttribute("xxl.job.shard.total", String.valueOf(shardTotal));
            }

        } catch (Exception e) {

        }
        
        // 记录开始执行
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行原方法
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            // 记录异常信息
            traceContext.setAttribute(TracerConstants.ERROR, "true");
            traceContext.setAttribute(TracerConstants.ERROR_MESSAGE, e.getMessage());
            log.error("XXL-Job task failed: {}, error: {}", jobName, e.getMessage(), e);
            
            // 向XXL-Job报告失败
            try {
                XxlJobHelper.handleFail(e.getMessage());
            } catch (Exception ex) {
                // 忽略非XXL-Job环境中的异常
            }
            
            throw e;
        } finally {
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            traceContext.setAttribute(TracerConstants.DURATION, String.valueOf(duration));
            TraceContext.removeContext();
        }
    }
} 