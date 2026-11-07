package com.example.qi.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参观行为事件（ADM-11），对应表 behavior_event。追加型数据。
 * 事件口径定义见主文档 7.11；enter_chapter / view_node 依赖 visitId 在会话内去重。
 */
@Data
@TableName("behavior_event")
public class BehaviorEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String anonymousId;

    private String visitId;

    /** enter_chapter / view_node / complete_chapter / click_relation / back_main */
    private String eventType;

    private Long chapterId;

    private Long nodeId;

    /** 关联跳转目标类型 / ID（click_relation 用） */
    private String targetType;

    private Long targetId;

    /** 数据库默认当前时间，代码不传 */
    private LocalDateTime occurTime;
}
