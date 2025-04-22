package com.example.tboxdemo.controller;

import org.springframework.web.bind.annotation.*;
import org.tbox.base.core.response.Result;
import org.tbox.base.core.response.Results;
import org.tbox.idempotent.annotation.Idempotent;
import org.tbox.idempotent.enums.IdempotentTypeEnum;

import java.util.UUID;

/**
 * 订单控制器
 * 用于演示幂等性注解功能
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * 创建订单 - 使用参数幂等
     * 
     * 幂等实现原理:
     * 1. 根据请求参数生成幂等ID
     * 2. 使用Redis存储幂等ID，设置过期时间
     * 3. 如果幂等ID已存在，则表示重复请求，抛出异常
     * 4. 如果幂等ID不存在，则正常处理业务逻辑
     */
    @PostMapping("/param")
    @Idempotent
    public Result<String> createOrderWithParamIdempotent(@RequestBody OrderRequest request) throws InterruptedException {
        // 模拟订单创建逻辑
        Thread.sleep(5000);
        String orderId = UUID.randomUUID().toString();
        return Results.success(orderId);
    }

    /**
     * 创建订单 - 使用SpEL表达式指定幂等键
     * 
     * 幂等实现原理:
     * 1. 根据SpEL表达式计算幂等ID
     * 2. 使用Redis存储幂等ID，设置过期时间
     * 3. 如果幂等ID已存在，则表示重复请求，抛出异常
     * 4. 如果幂等ID不存在，则正常处理业务逻辑
     */
    @PostMapping("/spel")
    @Idempotent
    public Result<String> createOrderWithSpelIdempotent(@RequestBody OrderRequest request) {
        // 模拟订单创建逻辑
        String orderId = UUID.randomUUID().toString();
        return Results.success(orderId);
    }

    /**
     * 订单请求DTO
     */
    public static class OrderRequest {
        private String userId;
        private String productId;
        private Integer quantity;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
} 