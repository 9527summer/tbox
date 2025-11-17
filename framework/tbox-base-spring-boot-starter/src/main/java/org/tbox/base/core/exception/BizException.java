package org.tbox.base.core.exception;

import org.tbox.base.core.enums.StandardErrorCodeEnum;

/**
 * 业务异常
 */
public class BizException extends BaseException {

    private static final long serialVersionUID = 1L;


    public BizException(String errMessage) {
        super(StandardErrorCodeEnum.BIZ_ERROR.getCode(),errMessage);
    }

    public BizException(String errCode, String errMessage) {
        super(errCode, errMessage);
    }

    public BizException(String errMessage, Throwable e) {
        super(StandardErrorCodeEnum.BIZ_ERROR.getCode(),errMessage, e);
    }

    public BizException(String errCode, String errMessage, Throwable e) {
        super(errCode, errMessage, e);
    }
}
