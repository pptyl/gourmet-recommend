package com.gourmet.vo;

import com.gourmet.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人中心聚合数据。
 */
@Data
@Schema(description = "个人中心")
public class UserCenterVO implements Serializable {

    @Schema(description = "用户资料")
    private User user;

    @Schema(description = "个人数据统计")
    private UserStatisticVO statistic;

    @Schema(description = "未读消息数")
    private long unreadMessageCount;
}
