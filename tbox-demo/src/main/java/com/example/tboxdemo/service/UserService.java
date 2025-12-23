package com.example.tboxdemo.service;

import com.example.tboxdemo.entity.User;
import com.example.tboxdemo.mapper.UserMapper;
//import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.tbox.base.core.exception.BizException;
import org.tbox.base.core.utils.AssertUtils;
//import org.tbox.base.core.exception.BizException;
//import org.tbox.base.core.utils.AssertUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    
    private final UserMapper userMapper;
    
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    // 分页查询所有用户
    public List<User> findAllUsers(int pageNum, int pageSize) {
//        PageHelper.startPage(pageNum, pageSize);
        return userMapper.findAll();
    }
    
    // 根据ID查询用户
    public User findUserById(Long id) {
        // 使用AssertUtils判断
        AssertUtils.notNull(id, "用户ID不能为空");
        
        User user = userMapper.findById(id);
        if (user == null) {
            // 抛出业务异常演示
            throw new BizException("用户不存在");
        }
        return user;
    }
    
    // 创建用户
    public User createUser(User user) {
        AssertUtils.notNull(user, "用户信息不能为空");
        AssertUtils.notNull(user.getUsername(), "用户名不能为空");
        
        // 设置创建时间
        user.setCreateTime(LocalDateTime.now());
        
        // 插入用户
        userMapper.insert(user);
        return user;
    }
    
    // 更新用户
    public User updateUser(User user) {
        AssertUtils.notNull(user, "用户信息不能为空");
        AssertUtils.notNull(user.getId(), "用户ID不能为空");
        
        // 检查用户是否存在
        User existingUser = userMapper.findById(user.getId());
        if (existingUser == null) {
            throw new BizException("用户不存在");
        }
        
        // 更新用户
        userMapper.update(user);
        return userMapper.findById(user.getId());
    }
    
    // 删除用户
    public void deleteUser(Long id) {
        AssertUtils.notNull(id, "用户ID不能为空");
        
        // 检查用户是否存在
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new BizException("用户不存在");
        }
        
        userMapper.deleteById(id);
    }
} 