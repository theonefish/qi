package com.example.qi.module.chapter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 叙事节点新增/编辑请求（6.13 / C.7）。
 * 五类节点共用一个请求体，按 nodeType 校验各自必填字段：
 * TEXT 正文为富文本 HTML（图文混排，术语以 [[term:ID]] 文本标记内嵌）；
 * TIMELINE 必填 eventIds；QUOTE 必填 inheritorId + quoteText；
 * PROCESS 必填 craftId（content 填适用范围说明）；
 * BRANCH 必填 choices（≥2，分支叙事）。
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

    /** TEXT：富文本 HTML 正文（术语标记 [[term:ID]] 内嵌）；PROCESS 适用范围说明 */
    private String content;

    /** 资料来源 */
    private String source;

    // ---- PROCESS：独立配图列表 ----
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

    // ---- 分支叙事：流向覆盖与画布坐标（所有类型均可带） ----
    /** 显式流向覆盖；null=按 sortOrder 的下一节点。BRANCH 节点恒为空（强制选择） */
    private Long nextNodeId;

    /** 图编排画布坐标（x,y），后台分支编排页使用 */
    private String flowPos;

    // ---- BRANCH：分支抉择选项 ----
    /**
     * 保存语义（分支叙事设计稿 §2.2）：null=保留现有选项（ChapterEdit 表单保存不带此字段，
     * 避免覆盖分支编排页配置）；[] =清空；非空=整体替换（先删后插）。
     */
    private List<ChoiceItem> choices;

    @Data
    public static class ChoiceItem {

        /** 按钮文案 */
        private String label;

        /** 选中后到达的同章节点 */
        private Long targetNodeId;
    }

    @Data
    public static class MediaItem {

        /** 媒体资源 ID（先通过 /admin/media/upload 上传获得） */
        private Long mediaId;

        private String caption;
    }
}
