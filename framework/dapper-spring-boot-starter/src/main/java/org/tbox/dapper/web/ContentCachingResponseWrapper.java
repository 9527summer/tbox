package org.tbox.dapper.web;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * 响应包装器，用于缓存响应内容以便多次读取
 */
public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedContent = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean committed = false;

    public ContentCachingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.outputStream == null) {
            this.outputStream = new CachedServletOutputStream(getResponse().getOutputStream(), this.cachedContent);
        }
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.writer == null) {
            this.writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
        }
        return this.writer;
    }

    /**
     * 获取缓存的响应内容
     */
    public byte[] getContentAsByteArray() {
        return this.cachedContent.toByteArray();
    }

    /**
     * 将缓存的内容写入原始响应中
     */
    public void copyBodyToResponse() throws IOException {
        if (this.outputStream != null) {
            this.outputStream.flush();
        }
        if (this.writer != null) {
            this.writer.flush();
        }

        byte[] body = this.cachedContent.toByteArray();
        if (!isCommitted()) {
            // 设置内容长度，避免分块传输
            getResponse().setContentLength(body.length);
        }
        getResponse().getOutputStream().write(body);
        getResponse().getOutputStream().flush();
    }

    @Override
    public void flushBuffer() throws IOException {
        // 不要实际刷新到底层响应，只记录状态
        this.committed = true;
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        this.cachedContent.reset();
    }

    @Override
    public boolean isCommitted() {
        return this.committed || super.isCommitted();
    }

    /**
     * 内部类：缓存的ServletOutputStream实现
     */
    private static class CachedServletOutputStream extends ServletOutputStream {
        private final ServletOutputStream targetStream;
        private final ByteArrayOutputStream cachedStream;

        CachedServletOutputStream(ServletOutputStream targetStream, ByteArrayOutputStream cachedStream) {
            this.targetStream = targetStream;
            this.cachedStream = cachedStream;
        }

        @Override
        public void write(int b) throws IOException {
            cachedStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            cachedStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            cachedStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            // 只刷新缓存，不刷新目标流
            cachedStream.flush();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new UnsupportedOperationException("setWriteListener not supported");
        }
    }
} 