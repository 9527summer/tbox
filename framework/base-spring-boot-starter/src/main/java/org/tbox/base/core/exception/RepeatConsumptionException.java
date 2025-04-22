
package org.tbox.base.core.exception;


import org.tbox.base.core.enums.StandardErrorCodeEnum;

/**
 * 重复消费异常
 */

public class RepeatConsumptionException extends BaseException {


    private static final long serialVersionUID = 2633239527798840962L;

    public RepeatConsumptionException(String errMessage) {
        super(StandardErrorCodeEnum.REPEAT_CONSUMER_ERROR.getCode(),errMessage);
    }

    public RepeatConsumptionException(String errCode, String errMessage) {
        super(errCode, errMessage);
    }

    public RepeatConsumptionException(String errMessage, Throwable e) {
        super(StandardErrorCodeEnum.REPEAT_CONSUMER_ERROR.getCode(),errMessage, e);
    }

    public RepeatConsumptionException(String errCode, String errMessage, Throwable e) {
        super(errCode, errMessage, e);
    }
}
