package org.tbox.base.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.tbox.base.core.config.properties.TboxBaseProperties;
import org.tbox.base.core.context.ApplicationContextHolder;

/**
 * TBox基础组件自动配置类
 * <p>
 * 该类负责自动配置TBox框架的基础组件
 */
@Configuration
@EnableConfigurationProperties(TboxBaseProperties.class)
@Import({JacksonConfig.class, WebMvcConfig.class,GlobalCorsConfig.class})
public class BaseAutoConfiguration {

    /**
     * 创建ApplicationContextHolder Bean
     * <p>
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