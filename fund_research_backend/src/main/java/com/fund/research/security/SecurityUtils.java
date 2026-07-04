package com.fund.research.security;

import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    public static Long requireCurrentUserId() {
        return requireCurrentUser().getUserId();
    }

    public static Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getUserId();
        }
        return null;
    }

    public static void assertSelfOrAdmin(Long targetUserId) {
        AuthenticatedUser user = requireCurrentUser();
        if (targetUserId == null || (!user.isAdmin() && !targetUserId.equals(user.getUserId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己的数据");
        }
    }
}
