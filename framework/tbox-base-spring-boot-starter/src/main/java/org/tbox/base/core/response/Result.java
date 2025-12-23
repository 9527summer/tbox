package org.tbox.base.core.response;


import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;

import java.io.Serializable;


public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1913876204241008017L;


    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "200";

    /**
     * 错误返回码
     */
    public static final String ERROR_CODE = "400";

    /**
     * 返回码
     */
    @Schema(description = "返回码")
    private String code;

    /**
     * 返回消息
     */
    @Schema(description = "返回消息")
    private String text;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
