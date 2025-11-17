package org.tbox.dapper.web;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 请求包装器，用于缓存请求内容以便多次读取
 */
public class ContentCachingRequestWrapper extends HttpServletRequestWrapper {

    private byte[] cachedContent;
    private final ByteArrayOutputStream cachedOutputStream;
    private final ServletInputStream inputStream;
    private BufferedReader reader;
    private final int maxContentLength;

    /**
     * 创建一个新的请求包装器
     * 
     * @param request 原始请求
     * @throws IOException 如果读取输入流时发生I/O错误
     */
    public ContentCachingRequestWrapper(HttpServletRequest request) throws IOException {
        this(request, -1);
    }
    
    /**
     * 创建一个新的请求包装器，指定最大内容长度
     * 
     * @param request 原始请求
     * @param maxContentLength 最大内容长度限制，小于0表示不限制
     * @throws IOException 如果读取输入流时发生I/O错误
     */
    public ContentCachingRequestWrapper(HttpServletRequest request, int maxContentLength) throws IOException {
        super(request);
        this.maxContentLength = maxContentLength;
        this.cachedOutputStream = new ByteArrayOutputStream();
        
        // 缓存输入流内容
        byte[] buffer = new byte[4096];
        int bytesRead;
        int totalBytesRead = 0;
        boolean limitReached = false;
        
        inputStream = request.getInputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            if (maxContentLength > 0) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxContentLength) {
                    // 只写入maxContentLength指定的字节数
                    int remainingBytes = maxContentLength - (totalBytesRead - bytesRead);
                    if (remainingBytes > 0) {
                        cachedOutputStream.write(buffer, 0, remainingBytes);
                    }
                    limitReached = true;
                    break;
                }
            }
            cachedOutputStream.write(buffer, 0, bytesRead);
        }
        
        cachedContent = cachedOutputStream.toByteArray();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedServletInputStream(cachedContent);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }
        return reader;
    }

    public byte[] getContentAsByteArray() {
        return cachedContent;
    }

    /**
     * 获取最大内容长度限制
     * 
     * @return 最大内容长度，-1表示不限制
     */
    public int getMaxContentLength() {
        return maxContentLength;
    }

    /**
     * 内部类：缓存的ServletInputStream实现
     */
    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] cachedContent) {
            this.inputStream = new ByteArrayInputStream(cachedContent);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("setReadListener not supported");
        }
    }
} 