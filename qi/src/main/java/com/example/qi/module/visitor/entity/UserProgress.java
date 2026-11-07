package com.example.qi.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 匿名用户阅读进度（FR-05 断点续读 + 章节完成状态），对应表 user_progress。
 * 章节完成由用户点击“完成本章”触发，幂等（14.6）；序章同样记录完成但不计入进度分母（D-02）。
 */
@Data
@TableName("user_progress")
public class UserProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String anonymousId;

    private Long chapterId;

    /** 最后阅读节点（5.6 断点续读定位用） */
    private Long lastNodeId;

    private Integer completed;

    private LocalDateTime completeTime;

    private LocalDateTime lastReadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
