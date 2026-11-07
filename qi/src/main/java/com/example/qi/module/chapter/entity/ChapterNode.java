package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节叙事节点（6.13），对应表 chapter_node。
 * 四类节点共用一张表；类型相关的扩展数据见 node_quote / node_event_relation /
 * node_craft_relation / node_media_relation（节点与步骤分层口径，15.2）。
 */
@Data
@TableName("chapter_node")
public class ChapterNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;

    /** TEXT / TIMELINE / QUOTE / PROCESS（NodeType 枚举） */
    private String nodeType;

    private String title;

    /** 副标题（图文节点可选） */
    private String subtitle;

    /**
     * TEXT：正文（术语以轻量标记存储，D-26）；
     * PROCESS：适用范围说明（工序因器而异，6.13）。
     */
    private String content;

    /** 资料来源 */
    private String source;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
