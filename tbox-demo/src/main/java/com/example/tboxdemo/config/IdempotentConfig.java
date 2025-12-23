package com.example.tboxdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 幂等性配置类
 */
//@Configuration
public class IdempotentConfig {

    /**
     * 自定义用户ID提供者
     * 在实际应用中，可以从认证上下文中获取当前用户ID
     */
//    @Bean
//    public UserIdProvider userIdProvider() {
//        return () -> {
//            // 模拟从认证上下文中获取用户ID
//            // 实际应用中，可以通过Spring Security、JWT等方式获取
//            return "demo-user";
//        };
//    }
} 