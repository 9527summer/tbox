package com.example.tboxdemo.controller;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.tboxdemo.entity.User;
import com.example.tboxdemo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tbox.base.core.exception.BizException;
import org.tbox.base.core.response.Result;
import org.tbox.base.core.response.Results;
import org.tbox.base.redis.utils.LockUtils;
import org.tbox.distributedid.utils.IdUtils;

import java.util.Date;
import java.util.List;
@Tag(name = "用户管理", description = "用户增删改查相关接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 分页查询所有用户

    @GetMapping
    public Result<List<User>> getAllUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<User> users = userService.findAllUsers(pageNum, pageSize);
        return Results.success(users);
    }

    // 根据ID查询用户
    @Operation(summary = "获取用户详情", description = "根据用户ID查询详细信息")
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
//        User user = userService.findUserById(id);
        boolean b = LockUtils.tryLock("123");
        if(!b){
            throw new BizException("获取不到");
        }

        System.out.println(objectMapper.getRegisteredModuleIds());
        User user = new User();
        user.setId(IdUtils.nextId());
        user.setCreateTime(LocalDateTime.now());
        return Results.success(user);
    }

    // 创建用户
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return Results.success(createdUser);
    }


    // 更新用户
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return Results.success(updatedUser);
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Results.success();
    }

    // 演示抛出异常
    @GetMapping("/error")
    public Result<Void> triggerError() {
        throw new BizException("这是一个异常");
    }
}