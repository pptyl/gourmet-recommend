package com.gourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料参数。
 */
@Data
@Schema(description = "修改个人资料参数")
public class ProfileUpdateDTO {

    @Size(max = 30, message = "昵称最多 30 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话")
    private String contactPhone;
}
