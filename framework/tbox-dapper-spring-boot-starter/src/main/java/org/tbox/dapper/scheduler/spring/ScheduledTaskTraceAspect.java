package org.tbox.dapper.scheduler.spring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Scheduled;
import org.tbox.dapper.config.TracerProperties;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.lang.reflect.Method;

/**
 * Spring定时任务追踪切面
 * 自动拦截使用@Scheduled注解的方法，添加追踪上下文
 */
@Aspect
public class ScheduledTaskTraceAspect {

    private TracerProperties tracerProperties;

    public ScheduledTaskTraceAspect(TracerProperties tracerProperties) {
        this.tracerProperties = tracerProperties;
    }

    /**
     * 拦截所有使用@Scheduled注解的方法
     * @param joinPoint 切点
     * @return 原方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object traceScheduledTask(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();
        
        // 获取@Scheduled注解信息
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
//        String taskInfo = getScheduledTaskInfo(scheduled);
        
        // 创建追踪上下文
        TraceContext traceContext = TraceContext.createRootContext(tracerProperties.getApplicationName());
        traceContext.setAttribute(TracerConstants.COMPONENT_TYPE, "scheduled-task");
        traceContext.setAttribute(TracerConstants.RESOURCE_TYPE, "spring-scheduled");
        traceContext.setAttribute(TracerConstants.RESOURCE_NAME, className + "." + methodName);
//        traceContext.setAttribute("scheduled.task.info", taskInfo);
        
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
            throw e;
        } finally {
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            traceContext.setAttribute(TracerConstants.DURATION, String.valueOf(duration));
            TraceContext.removeContext();
        }
    }
    
    /**
     * 获取定时任务的调度信息
     * @param scheduled Scheduled注解
     * @return 调度信息描述
     */
    private String getScheduledTaskInfo(Scheduled scheduled) {
        StringBuilder info = new StringBuilder();
        
        if (scheduled.cron() != null && !scheduled.cron().isEmpty()) {
            info.append("cron='").append(scheduled.cron()).append("'");
        } else if (scheduled.fixedDelay() > 0) {
            info.append("fixedDelay=").append(scheduled.fixedDelay()).append("ms");
        } else if (scheduled.fixedDelayString() != null && !scheduled.fixedDelayString().isEmpty()) {
            info.append("fixedDelayString='").append(scheduled.fixedDelayString()).append("'");
        } else if (scheduled.fixedRate() > 0) {
            info.append("fixedRate=").append(scheduled.fixedRate()).append("ms");
        } else if (scheduled.fixedRateString() != null && !scheduled.fixedRateString().isEmpty()) {
            info.append("fixedRateString='").append(scheduled.fixedRateString()).append("'");
        } else {
            info.append("unspecified");
        }
        
        return info.toString();
    }
} 