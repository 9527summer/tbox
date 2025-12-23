
package org.tbox.base.core.response;



public final class Results {

    /**
     * 构造成功响应
     */
    public static Result<Void> success() {
        Result<Void> result = new Result<>();
        result.setCode(Result.SUCCESS_CODE);
        return result;
    }

    /**
     * 构造带返回数据的成功响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(Result.SUCCESS_CODE);
        result.setData(data);
        return result;
    }


    /**
     * 构建服务端失败响应
     */
    public static Result<Void> failure() {
        Result<Void> result = new Result<>();
        result.setCode(Result.ERROR_CODE);
        result.setText("系统错误");
        return result;
    }



    /**
     * 通过 errorCode、errorMessage 构建失败响应
     */
    public static Result<Void> failure(String errorCode, String errorMessage) {
        Result<Void> result = new Result<>();
        result.setCode(errorCode);
        result.setText(errorMessage);
        return result;
    }

}
