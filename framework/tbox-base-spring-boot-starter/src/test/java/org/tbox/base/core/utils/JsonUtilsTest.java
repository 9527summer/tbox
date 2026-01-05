package org.tbox.base.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.tbox.base.core.exception.SysException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonUtils 单元测试
 */
class JsonUtilsTest {

    static class User {
        private String name;
        private int age;
        private LocalDateTime createTime;

        public User() {}

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    @Test
    void testToJson_SimpleObject() {
        User user = new User("张三", 25);
        String json = JsonUtils.toJson(user);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"张三\""));
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    void testToJson_NullObject() {
        String json = JsonUtils.toJson(null);
        assertEquals("null", json);
    }

    @Test
    void testFromJson_SimpleObject() {
        String json = "{\"name\":\"李四\",\"age\":30}";
        User user = JsonUtils.fromJson(json, User.class);

        assertNotNull(user);
        assertEquals("李四", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void testFromJson_IgnoreUnknownProperties() {
        String json = "{\"name\":\"王五\",\"age\":35,\"unknownField\":\"value\"}";
        User user = JsonUtils.fromJson(json, User.class);

        assertNotNull(user);
        assertEquals("王五", user.getName());
        assertEquals(35, user.getAge());
    }

    @Test
    void testFromJson_InvalidJson() {
        String invalidJson = "not a json";
        assertThrows(SysException.class, () -> JsonUtils.fromJson(invalidJson, User.class));
    }

    @Test
    void testFromJson_TypeReference() {
        String json = "[{\"name\":\"用户1\",\"age\":20},{\"name\":\"用户2\",\"age\":25}]";
        List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("用户1", users.get(0).getName());
        assertEquals("用户2", users.get(1).getName());
    }

    @Test
    void testToList() {
        String json = "[{\"name\":\"A\",\"age\":1},{\"name\":\"B\",\"age\":2}]";
        List<User> users = JsonUtils.toList(json, User.class);

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("A", users.get(0).getName());
        assertEquals("B", users.get(1).getName());
    }

    @Test
    void testToList_EmptyList() {
        String json = "[]";
        List<User> users = JsonUtils.toList(json, User.class);

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void testToBytes_AndFromBytes() {
        User user = new User("测试用户", 28);
        byte[] bytes = JsonUtils.toBytes(user);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        User restored = JsonUtils.fromBytes(bytes, User.class);
        assertNotNull(restored);
        assertEquals("测试用户", restored.getName());
        assertEquals(28, restored.getAge());
    }

    @Test
    void testToMap() {
        String json = "{\"key1\":\"value1\",\"key2\":123,\"key3\":true}";
        Map<String, Object> map = JsonUtils.toMap(json);

        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals(123, map.get("key2"));
        assertEquals(true, map.get("key3"));
    }

    @Test
    void testToMap_NestedObject() {
        String json = "{\"outer\":{\"inner\":\"value\"}}";
        Map<String, Object> map = JsonUtils.toMap(json);

        assertNotNull(map);
        assertTrue(map.get("outer") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> outer = (Map<String, Object>) map.get("outer");
        assertEquals("value", outer.get("inner"));
    }

    @Test
    void testGetObjectMapper() {
        assertNotNull(JsonUtils.getObjectMapper());
        assertSame(JsonUtils.getObjectMapper(), JsonUtils.getObjectMapper());
    }

    @Test
    void testJava8TimeSupport() {
        User user = new User("时间测试", 30);
        user.setCreateTime(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        String json = JsonUtils.toJson(user);
        assertNotNull(json);
        assertFalse(json.matches(".*\"createTime\":\\d+.*"));

        User restored = JsonUtils.fromJson(json, User.class);
        assertNotNull(restored.getCreateTime());
        assertEquals(2024, restored.getCreateTime().getYear());
        assertEquals(1, restored.getCreateTime().getMonthValue());
        assertEquals(15, restored.getCreateTime().getDayOfMonth());
    }

    @Test
    void testSerializeDeserialize_ComplexObject() {
        List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
        String json = JsonUtils.toJson(tags);
        List<String> restored = JsonUtils.fromJson(json, new TypeReference<List<String>>() {});

        assertEquals(tags, restored);
    }
}

