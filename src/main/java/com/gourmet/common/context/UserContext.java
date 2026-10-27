package com.gourmet.common.context;

import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.model.LoginUser;
import com.gourmet.common.result.ResultCode;

/**
 * 当前登录用户上下文（ThreadLocal）。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 获取当前登录账号 ID，未登录直接抛出 401。
     */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        if (user == null || user.getId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user.getId();
    }

    /**
     * 获取当前登录账号 ID，未登录返回 null（用于公开接口的个性化增强）。
     */
    public static Long getUserIdOrNull() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isMerchant() {
        LoginUser user = HOLDER.get();
        return user != null && user.isMerchant();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && user.isAdmin();
    }

    /**
     * 商家后台专用：当前商家 ID。
     */
    public static Long getMerchantId() {
        LoginUser user = HOLDER.get();
        if (user == null || !user.isMerchant()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅商家账号可执行该操作");
        }
        return user.getId();
    }
}
