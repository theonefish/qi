package com.example.qi.module.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 完成本章请求（FR-03 / 6.5）：章节完成由用户点击触发，幂等。
 */
@Data
public class CompleteChapterRequest {

    @NotBlank(message = "缺少匿名标识")
    private String anonymousId;

    @NotNull(message = "缺少章节 ID")
    private Long chapterId;
}
