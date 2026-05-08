package com.lyllink.proofly.utils;

import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserHolder {

    private CurrentUserHolder() {
    }

    public static CurrentUser required() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw BusinessException.unauthorized("未登录或登录已失效");
        }
        return currentUser;
    }
}
