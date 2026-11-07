package com.example.qi.module.relation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容对象关联（ADM-09），对应表 content_relation。
 * 多对多统一关联表，不使用自由 JSON 字段（8.3）。
 */
@Data
@TableName("content_relation")
public class ContentRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** chapter / artifact / craft / inheritor / term / event / motif */
    private String sourceType;

    private Long sourceId;

    private String targetType;

    private Long targetId;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
