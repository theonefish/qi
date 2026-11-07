package com.example.qi.module.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏请求（FR-09；P0 仅藏品，D-16）。
 */
@Data
public class FavoriteRequest {

    @NotBlank(message = "缺少匿名标识")
    private String anonymousId;

    @NotBlank(message = "缺少收藏目标类型")
    private String targetType;

    @NotNull(message = "缺少收藏目标 ID")
    private Long targetId;
}
