package com.gourmet.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录用户信息（由 JWT 解析得到，存放于 ThreadLocal）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    /** 账号主键：用户 ID / 商家 ID / 管理员 ID */
    private Long id;

    /** 角色：USER / MERCHANT / ADMIN */
    private String role;

    /** 昵称或店铺名 */
    private String name;

    public boolean isUser() {
        return "USER".equals(role);
    }

    public boolean isMerchant() {
        return "MERCHANT".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
