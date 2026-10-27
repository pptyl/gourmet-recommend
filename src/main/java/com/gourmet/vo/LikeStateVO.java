package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 点赞状态。
 */
@Data
@AllArgsConstructor
@Schema(description = "点赞状态")
public class LikeStateVO implements Serializable {

    @Schema(description = "当前用户是否已点赞")
    private boolean liked;

    @Schema(description = "点赞总数")
    private long count;
}
