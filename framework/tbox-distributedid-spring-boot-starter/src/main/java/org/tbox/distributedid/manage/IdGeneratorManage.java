package org.tbox.distributedid.manage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdGeneratorManage {

    public static final String DEFAULT_KEY = "default";
    public static final String TIME_KEY = "time";

    private static Map<String,IdGenerator> idGeneratorMap = new ConcurrentHashMap<>();

    static {
        idGeneratorMap.put(DEFAULT_KEY, new DefaultIdGenerator());
        idGeneratorMap.put(TIME_KEY, new TimeIdGenerator());
    }

    /**
     * 获取 ID 生成器
     * @param resource
     * @return
     */
    public static IdGenerator getIdGenerator(String resource) {
        return idGeneratorMap.get(resource);
    }

    /**
     * 获取默认 ID 生成器
     * @return
     */
    public static IdGenerator getDefaultServiceIdGenerator() {
        return getIdGenerator(DEFAULT_KEY);
    }
    
    /**
     * 获取可读性 ID 生成器
     * @return
     */
    public static IdGenerator getTimeIdGenerator() {
        return getIdGenerator(TIME_KEY);
    }
}
