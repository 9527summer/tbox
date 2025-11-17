package org.tbox.distributedid.manage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdGeneratorManage {

    private static Map<String,IdGenerator> idGeneratorMap = new ConcurrentHashMap<>();

    static {
        idGeneratorMap.put("default", new DefaultIdGenerator());
    }

//    public static void registerIdGenerator(String resource, IdGenerator idGenerator) {
//        IdGenerator generator = idGeneratorMap.get(resource);
//        if (generator!=null) {
//            idGeneratorMap.put(resource, idGenerator);
//        }
//    }

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
        return getIdGenerator("default");
    }
}
