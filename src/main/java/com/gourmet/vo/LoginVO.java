package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录返回结果。
 */
@Data
@Schema(description = "登录结果")
public class LoginVO implements Serializable {

    @Schema(description = "JWT 令牌")
    private String token;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";

    @Schema(description = "有效期（分钟）")
    private long expiresIn;

    @Schema(description = "角色：USER/MERCHANT/ADMIN")
    private String role;

    @Schema(description = "账号 ID")
    private Long accountId;

    @Schema(description = "昵称 / 店铺名")
    private String name;

    @Schema(description = "头像")
    private String avatar;

    public static LoginVO of(String token, String role, Long accountId, String name, String avatar, long expiresIn) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRole(role);
        vo.setAccountId(accountId);
        vo.setName(name);
        vo.setAvatar(avatar);
        vo.setExpiresIn(expiresIn);
        return vo;
    }
}
