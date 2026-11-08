package com.example.qi.module.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 阅读位置上报（5.6 断点续读）：用户实际浏览到某节点时调用。
 */
@Data
public class ReadPositionRequest {

    @NotBlank(message = "缺少匿名标识")
    private String anonymousId;

    @NotNull(message = "缺少章节 ID")
    private Long chapterId;

    @NotNull(message = "缺少节点 ID")
    private Long nodeId;

    /** 分支选择路径 JSON（可选，分支叙事断点续读；形如 [{"n":3,"c":1}]） */
    private String choicePath;
}
