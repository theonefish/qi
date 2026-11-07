package com.example.qi.module.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 章节发布请求（7.14：修改内容必填）。
 */
@Data
public class ChapterPublishRequest {

    @NotBlank(message = "修改内容说明不能为空")
    private String modifySummary;
}
