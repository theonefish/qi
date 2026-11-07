package com.example.qi.module.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 章节新增/编辑请求（ADM-03）。
 * 封面/摘要允许草稿阶段留空，发布校验时强制（5.2）。
 */
@Data
public class ChapterSaveRequest {

    /** 为空表示新增，编辑时必传 */
    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String subtitle;

    private String coverUrl;

    private String summary;

    /** 0 序章 / 1 正章（D-02） */
    @NotNull(message = "章节类型不能为空")
    private Integer chapterType;

    private Integer sortOrder;
}
