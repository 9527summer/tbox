package org.tbox.base.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class WebUtils {

    private static final Logger log = LoggerFactory.getLogger(WebUtils.class);
    /**
     * 获取当前请求的ServletPath
     *
     * @return 当前请求的ServletPath，如果不在Web上下文中则返回null或默认值
     */
    public static String getServletPath() {
        return getServletPath(null);
    }

    /**
     * 获取当前请求的ServletPath
     *
     * @param defaultPath 不在Web上下文时返回的默认值
     * @return 当前请求的ServletPath或默认值
     */
    public static String getServletPath(String defaultPath) {

        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            if (request != null) {
                return request.getServletPath();
            }
        }
        return defaultPath;

    }

    /**
     * 获取当前请求的完整URL
     */
    public static String getRequestUrl() {

        ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            if (request != null) {
                return request.getRequestURL().toString();
            }
        }
        return null;


    }

    /**
     * 获取当前请求的参数值
     * @param paramName 参数名
     * @return 参数值，不存在或不在Web上下文中返回null
     */
    public static String getParameter(String paramName) {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getParameter(paramName);
            }
        } catch (Exception e) {
            log.debug("Failed to get request parameter: {}", paramName, e);
        }
        return null;
    }

    /**
     * 获取当前请求中的请求头
     * @param headerName 请求头名称
     * @return 请求头值，不存在或不在Web上下文中返回null
     */
    public static String getHeader(String headerName) {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                return requestAttributes.getRequest().getHeader(headerName);
            }
        } catch (Exception e) {
            log.debug("Failed to get request header: {}", headerName, e);
        }
        return null;
    }

    /**
     * 获取客户端IP地址
     * @return 客户端IP地址，不在Web上下文中返回null
     */
    public static String getClientIp() {
        try {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_CLIENT_IP");
                }
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("Failed to get client IP", e);
        }
        return null;
    }
}

