package org.tbox.dapper.utils;

/**
 * 路径匹配工具类
 * 支持Ant风格的路径匹配，如：/api/**, /api/v1/*.json
 */
public class PathMatcher {
    
    /**
     * 判断路径是否匹配指定的模式
     *
     * @param pattern 匹配模式，支持*和**通配符
     * @param path    需要匹配的路径
     * @return 是否匹配
     */
    public static boolean match(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        
        // 精确匹配
        if (pattern.equals(path)) {
            return true;
        }
        
        // 处理 /** 结尾的情况
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        
        // 处理 /* 结尾的情况
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (!path.startsWith(prefix)) {
                return false;
            }
            
            // 确保路径中不包含更深层次的目录
            String remaining = path.substring(prefix.length());
            return !remaining.contains("/") || remaining.equals("/");
        }
        
        // 处理中间有 * 的情况
        if (pattern.contains("*")) {
            return matchWithWildcards(pattern, path);
        }
        
        return false;
    }
    
    /**
     * 使用通配符进行匹配
     *
     * @param pattern 包含通配符的模式
     * @param path    需要匹配的路径
     * @return 是否匹配
     */
    private static boolean matchWithWildcards(String pattern, String path) {
        // 简单的通配符匹配实现
        String[] patternParts = pattern.split("\\*", -1);
        int index = 0;
        
        for (String part : patternParts) {
            if (part.isEmpty()) {
                continue;
            }
            
            index = path.indexOf(part, index);
            if (index == -1) {
                return false;
            }
            
            index += part.length();
        }
        
        // 如果模式以*结尾，或者已经匹配到路径末尾，则认为匹配成功
        return pattern.endsWith("*") || index == path.length();
    }
} 