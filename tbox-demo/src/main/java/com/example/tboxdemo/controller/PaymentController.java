package com.example.tboxdemo.controller;

import org.springframework.web.bind.annotation.*;
import org.tbox.base.core.response.Result;
import org.tbox.base.core.response.Results;
import org.tbox.idempotent.annotation.Idempotent;
import org.tbox.idempotent.enums.IdempotentTypeEnum;

import java.util.UUID;

/**
 * 支付控制器
 * 用于演示幂等性注解功能 - 分布式锁方式
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    /**
     * 创建支付 - 使用分布式锁方式实现幂等
     * 
     * 幂等实现原理:
     * 1. 根据请求参数生成分布式锁key
     * 2. 使用Redis尝试获取分布式锁
     * 3. 如果获取锁成功，则执行业务逻辑，然后释放锁
     * 4. 如果获取锁失败，则表示重复请求，抛出异常
     */
    @PostMapping
    @Idempotent
    public Result<String> createPayment(@RequestBody PaymentRequest request) {
        // 模拟支付创建逻辑 - 这里会有一个30秒的锁，防止重复支付
        try {
            // 模拟处理时间
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String paymentId = UUID.randomUUID().toString();
        return Results.success(paymentId);
    }

    /**
     * 支付请求DTO
     */
    public static class PaymentRequest {
        private String orderId;
        private Double amount;
        private String paymentMethod;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }
} 