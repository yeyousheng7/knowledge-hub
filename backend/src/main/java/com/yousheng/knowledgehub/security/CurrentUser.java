package com.yousheng.knowledgehub.security;

import com.yousheng.knowledgehub.common.exception.BizException;
import com.yousheng.knowledgehub.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    public static Long getUserId() {
        return getPrincipal().userId();
    }

    public static String getUsername() {
        return getPrincipal().username();
    }

    public static String getRole() {
        return getPrincipal().role();
    }

    public static CurrentUserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CurrentUserPrincipal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        return (CurrentUserPrincipal) principal;
    }
}
