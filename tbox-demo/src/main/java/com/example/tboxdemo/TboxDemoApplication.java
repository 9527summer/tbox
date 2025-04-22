package com.example.tboxdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
@MapperScan("com.example.tboxdemo.mapper")
public class TboxDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TboxDemoApplication.class, args);
    }
} 