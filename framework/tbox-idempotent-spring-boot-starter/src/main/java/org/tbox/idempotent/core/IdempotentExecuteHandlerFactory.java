

package org.tbox.idempotent.core;


import org.tbox.base.core.context.ApplicationContextHolder;
import org.tbox.idempotent.core.param.IdempotentParamService;
import org.tbox.idempotent.enums.IdempotentTypeEnum;

/**
 * 幂等执行处理器工厂
 */
public final class IdempotentExecuteHandlerFactory {

    /**
     * 获取幂等执行处理器
     *
     * @param type  指定幂等处理类型
     * @return 幂等执行处理器
     */
    public static IdempotentExecuteHandler getInstance(IdempotentTypeEnum type) {
        IdempotentExecuteHandler result = null;
        switch (type){
            case PARAM:
                result = ApplicationContextHolder.getBean(IdempotentParamService.class);
        }
        return result;
    }
}
