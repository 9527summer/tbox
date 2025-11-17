package org.tbox.base.core.exception;

import org.tbox.base.core.enums.StandardErrorCodeEnum;

/**
 * 系统异常
 */
public class SysException extends BaseException {

    private static final long serialVersionUID = 1L;

    public SysException(String errMessage) {
        super(StandardErrorCodeEnum.SYSTEM_ERROR.getCode(),errMessage);
    }

    public SysException(String errCode, String errMessage) {
        super(errCode, errMessage);
    }

    public SysException(String errMessage, Throwable e) {
        super(StandardErrorCodeEnum.SYSTEM_ERROR.getCode(),errMessage, e);
    }

    public SysException(String errCode, String errMessage, Throwable e) {
        super(errCode, errMessage, e);
    }
}
