package org.tbox.dapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TBox-Tracer 系统配置参数
 */
@ConfigurationProperties(prefix = "tbox.tracer")
public class TracerProperties {
    
    /**
     * 是否启用追踪
     */
    private boolean enabled = true;
    
    /**
     * 应用名称，默认从spring.application.name获取
     */
    private String applicationName;
    
    /**
     * 是否打印请求和响应内容
     */
    private boolean printPayload = true;
    
    /**
     * 响应结果最大长度，日志记录超过此长度会被截断
     */
    private int maxResponseLength = 2048;
    

    /**
     * 用户添加的额外排除路径
     */
    private String[] excludePaths = {};
    
    /**
     * 调度任务追踪配置
     */
    private SchedulerConfig scheduler = new SchedulerConfig();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public boolean isPrintPayload() {
        return printPayload;
    }
    
    public void setPrintPayload(boolean printPayload) {
        this.printPayload = printPayload;
    }
    
    public int getMaxResponseLength() {
        return maxResponseLength;
    }
    
    public void setMaxResponseLength(int maxResponseLength) {
        this.maxResponseLength = maxResponseLength;
    }

    /**
     * 获取用户配置的额外排除路径
     */
    public String[] getExcludePaths() {
        return excludePaths;
    }
    
    /**
     * 设置用户配置的额外排除路径
     */
    public void setExcludePaths(String[] excludePaths) {
        this.excludePaths = excludePaths;
    }
    
    /**
     * 获取调度任务追踪配置
     */
    public SchedulerConfig getScheduler() {
        return scheduler;
    }
    
    /**
     * 设置调度任务追踪配置
     */
    public void setScheduler(SchedulerConfig scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 获取合并后的所有排除路径（默认路径+用户配置路径）
     */
    public String[] getAllExcludePaths() {

        String[] defaultExcludePaths = {
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/favicon.ico",
                "/error"
        };

        if (excludePaths == null || excludePaths.length == 0) {
            return defaultExcludePaths;
        }
        
        List<String> allPaths = new ArrayList<>(Arrays.asList(defaultExcludePaths));
        allPaths.addAll(Arrays.asList(excludePaths));
        return allPaths.toArray(new String[0]);
    }
    
    /**
     * 调度任务追踪配置
     */
    public static class SchedulerConfig {
        /**
         * 是否启用Spring @Scheduled注解的任务追踪
         */
        private boolean springScheduledEnabled = true;
        
        /**
         * 是否启用XXL-Job任务追踪
         */
        private boolean xxljobEnabled = true;
        
        /**
         * 是否启用Quartz任务追踪
         */
        private boolean quartzEnabled = true;
        
        public boolean isSpringScheduledEnabled() {
            return springScheduledEnabled;
        }
        
        public void setSpringScheduledEnabled(boolean springScheduledEnabled) {
            this.springScheduledEnabled = springScheduledEnabled;
        }
        
        public boolean isXxljobEnabled() {
            return xxljobEnabled;
        }
        
        public void setXxljobEnabled(boolean xxljobEnabled) {
            this.xxljobEnabled = xxljobEnabled;
        }
        
        public boolean isQuartzEnabled() {
            return quartzEnabled;
        }
        
        public void setQuartzEnabled(boolean quartzEnabled) {
            this.quartzEnabled = quartzEnabled;
        }
    }
}
