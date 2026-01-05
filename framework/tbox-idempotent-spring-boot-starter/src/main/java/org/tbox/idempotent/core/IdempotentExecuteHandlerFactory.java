

package org.tbox.idempotent.core;


import org.tbox.base.core.exception.SysException;
import org.tbox.idempotent.core.param.IdempotentParamService;
import org.tbox.idempotent.core.spel.IdempotentSpelService;
import org.tbox.idempotent.enums.IdempotentTypeEnum;

/**
 * 幂等执行处理器工厂
 */
public final class IdempotentExecuteHandlerFactory {

    private final IdempotentParamService idempotentParamService;
    private final IdempotentSpelService idempotentSpelService;

    public IdempotentExecuteHandlerFactory(IdempotentParamService idempotentParamService, IdempotentSpelService idempotentSpelService) {
        this.idempotentParamService = idempotentParamService;
        this.idempotentSpelService = idempotentSpelService;
    }

    /**
     * 获取幂等执行处理器
     *
     * @param type  指定幂等处理类型
     * @return 幂等执行处理器
     * @throws SysException 如果type为null或不支持的类型
     */
    public IdempotentExecuteHandler getInstance(IdempotentTypeEnum type) {
        if (type == null) {
            throw new SysException("幂等类型不能为空");
        }
        IdempotentExecuteHandler result;
        switch (type) {
            case PARAM:
                result = idempotentParamService;
                break;
            case SPEL:
                result = idempotentSpelService;
                break;
            default:
                throw new SysException("不支持的幂等类型: " + type);
        }
        return result;
    }
}
