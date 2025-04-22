package com.example.tboxdemo.mapper;

import com.example.tboxdemo.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Insert("INSERT INTO users(username, email, phone, create_time) VALUES(#{username}, #{email}, #{phone}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET username = #{username}, email = #{email}, phone = #{phone} WHERE id = #{id}")
    int update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);
} 