package org.tbox.base.core.response;


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
    private String code;

    /**
     * 返回消息
     */
    private String text;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;


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

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public PageInfo getPageInfo(){
        return pageInfo;
    }
//
//    public String getRequestId(){
//        return MDC.get(TLogConstants.TLOG_TRACE_KEY);
//    }


    public static class PageInfo{

        private Long currentPageNo;

        private Long pageSize;

        private Long total;

        public PageInfo() {
        }

        public PageInfo(Long currentPageNo, Long pageSize, Long total) {
            this.currentPageNo = currentPageNo;
            this.pageSize = pageSize;
            this.total = total;
        }

        public Long getCurrentPageNo() {
            return currentPageNo;
        }

        public void setCurrentPageNo(Long currentPageNo) {
            this.currentPageNo = currentPageNo;
        }

        public Long getPageSize() {
            return pageSize;
        }

        public void setPageSize(Long pageSize) {
            this.pageSize = pageSize;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }
    }



}
