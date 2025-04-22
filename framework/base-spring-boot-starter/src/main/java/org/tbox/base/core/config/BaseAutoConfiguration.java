package org.tbox.base.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.tbox.base.core.context.ApplicationContextHolder;

/**
 * TBox基础组件自动配置类
 * 
 * 该类负责自动配置TBox框架的基础组件
 * 通过@Configuration标记为Spring配置类
 * 通过@ComponentScan确保org.tbox.base包下的组件被自动扫描和注册
 */
@Configuration
@ComponentScan(basePackages = {"org.tbox.base"})
public class BaseAutoConfiguration {
    
    /**
     * 创建ApplicationContextHolder Bean
     * 
     * 确保ApplicationContextHolder被Spring容器管理，
     * 使其能够获取到ApplicationContext引用
     * 
     * @return ApplicationContextHolder实例
     */
    @Bean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }
} 