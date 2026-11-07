package com.example.qi.module.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 叙事节点新增/编辑请求（6.13 / C.7）。
 * 四类节点共用一个请求体，按 nodeType 校验各自必填字段：
 * TEXT 必填 content；TIMELINE 必填 eventIds；QUOTE 必填 inheritorId + quoteText；
 * PROCESS 必填 craftId（content 填适用范围说明）。
 */
@Data
public class NodeSaveRequest {

    /** 为空表示新增，编辑时必传 */
    private Long id;

    /** TEXT / TIMELINE / QUOTE / PROCESS */
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @NotBlank(message = "节点标题不能为空")
    private String title;

    private String subtitle;

    /** TEXT 正文（含术语轻量标记，D-26）；PROCESS 适用范围说明 */
    private String content;

    /** 资料来源 */
    private String source;

    // ---- TEXT / PROCESS：图片列表 ----
    private List<MediaItem> media;

    // ---- TIMELINE：引用的历史事件（有序） ----
    private List<Long> eventIds;

    // ---- QUOTE：口述扩展 ----
    private Long inheritorId;

    private String quoteText;

    private String quoteSource;

    private String authorizationNote;

    // ---- PROCESS：引用的工艺与工序 ----
    private Long craftId;

    /** 引用的工序（有序），可空表示整工艺概览 */
    private List<Long> craftStepIds;

    @Data
    public static class MediaItem {

        /** 媒体资源 ID（先通过 /admin/media/upload 上传获得） */
        private Long mediaId;

        private String caption;
    }
}
