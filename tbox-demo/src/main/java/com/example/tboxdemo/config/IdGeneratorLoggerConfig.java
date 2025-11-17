package com.example.tboxdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbox.distributedid.core.Snowflake;
import org.tbox.distributedid.utils.IdUtils;

import java.util.Date;

/**
 * 分布式ID生成器日志配置
 * 在应用启动时输出分布式ID生成器的配置和状态
 */
@Configuration
public class IdGeneratorLoggerConfig {

    private static final Logger logger = LoggerFactory.getLogger(IdGeneratorLoggerConfig.class);

    @Bean
    public CommandLineRunner idGeneratorLogger() {
        return args -> {
            try {
                Snowflake snowflake = IdUtils.getInstance();
                if (snowflake != null) {
                    // 生成一个测试ID
                    long testId = snowflake.nextId();
                    
                    logger.info("==================== 分布式ID生成器状态 ====================");
                    logger.info("雪花算法实例已初始化");
                    logger.info("测试ID生成: {}", testId);
                    
                    // 获取生成的ID信息
                    long workerId = snowflake.getWorkerId(testId);
                    long dataCenterId = snowflake.getDataCenterId(testId);
                    long timestamp = snowflake.getGenerateDateTime(testId);
                    
                    logger.info("工作节点ID (workerId): {}", workerId);
                    logger.info("数据中心ID (dataCenterId): {}", dataCenterId);
                    logger.info("ID生成时间: {}", new Date(timestamp));
                    
                    // 测试短时间内生成多个ID是否有序
                    logger.info("短时间内生成多个ID测试:");
                    for (int i = 0; i < 5; i++) {
                        logger.info("ID {}: {}", i + 1, snowflake.nextId());
                    }
                    
                    logger.info("==========================================================");
                } else {
                    logger.warn("雪花算法实例未初始化，分布式ID生成器可能未正确配置");
                }
            } catch (Exception e) {
                logger.error("检查分布式ID生成器状态时发生错误", e);
            }
        };
    }
} 