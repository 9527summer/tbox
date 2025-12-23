package org.tbox.base.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
@ConditionalOnProperty(prefix = "tbox.base", name = "defaultJacksonConfigEnable", havingValue = "true", matchIfMissing = true)
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 1. 基础配置
            builder.timeZone(TimeZone.getDefault());
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // 2. 创建 JavaTimeModule（时间序列化/反序列化）
            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // === 序列化（Java → JSON）===
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                    DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(
                    DateTimeFormatter.ofPattern(DATE_PATTERN)));
            javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(
                    DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // === 反序列化（JSON → Java）===
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
                    DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(
                    DateTimeFormatter.ofPattern(DATE_PATTERN)));
            javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(
                    DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // 3. 创建 SimpleModule（Long/BigInteger 转 String）
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
            simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);

            builder.modules(javaTimeModule, simpleModule);
        };
    }
}
