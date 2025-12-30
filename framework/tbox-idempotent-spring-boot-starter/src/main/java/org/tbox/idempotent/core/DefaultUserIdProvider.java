package org.tbox.idempotent.core;

import org.springframework.util.StringUtils;

import org.tbox.base.core.utils.WebUtils;


/**
 * 默认实现
 */
public class DefaultUserIdProvider implements UserIdProvider {

    @Override
    public String getCurrentUserId() {
        String authorization = WebUtils.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            authorization = authorization.replace("Bearer ", "");
        }
        return authorization;
    }

}
