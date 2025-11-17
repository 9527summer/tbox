package org.tbox.base.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.tbox.base.core.exception.SysException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtils {

    // 创建线程安全的ObjectMapper实例
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 配置ObjectMapper
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 注册Java8时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * 将对象序列化为JSON字符串
     *
     * @param obj 要序列化的对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new SysException("序列化对象为JSON失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为对象
     *
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new SysException("反序列化JSON失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为复杂类型对象
     *
     * @param json JSON字符串
     * @param typeReference 类型引用
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new SysException("反序列化JSON失败", e);
        }
    }

    /**
     * 将JSON字符串反序列化为List
     *
     * @param json JSON字符串
     * @param elementClass List元素类型
     * @return List对象
     */
    public static <T> List<T> toList(String json, Class<T> elementClass) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new SysException("反序列化JSON到List失败", e);
        }
    }

    /**
     * 将对象转换为字节数组
     *
     * @param obj 要转换的对象
     * @return 字节数组
     */
    public static byte[] toBytes(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new SysException("序列化对象为字节数组失败", e);
        }
    }

    /**
     * 将字节数组反序列化为对象
     *
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @return 反序列化后的对象
     */
    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new SysException("反序列化字节数组失败", e);
        }
    }

    /**
     * 将JSON字符串转换为Map
     *
     * @param json JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> toMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new SysException("反序列化JSON到Map失败", e);
        }
    }

    /**
     * 获取ObjectMapper实例
     * 注意：返回的是同一个实例，ObjectMapper是线程安全的
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
