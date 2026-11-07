package com.example.qi.module.relation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 关联保存请求（ADM-09）：为内容对象整体替换关联列表。
 * 章节、藏品等模块共用。
 */
@Data
public class RelationSaveRequest {

    private List<TargetItem> targets;

    @Data
    public static class TargetItem {

        /** 目标类型：chapter / artifact / craft / inheritor / term / event / motif */
        @NotBlank(message = "目标类型不能为空")
        private String targetType;

        @NotNull(message = "目标 ID 不能为空")
        private Long targetId;
    }
}
