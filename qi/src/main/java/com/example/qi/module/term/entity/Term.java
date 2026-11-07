package com.example.qi.module.term.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业术语（ADM-07 / FR-06），对应表 term。
 * 发布条件：名称、释义、审核记录（5.2）；未审核或无释义的术语前台不显示点击标记。
 */
@Data
@TableName("term")
public class Term {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String definition;

    private String source;

    /** 0 未审核 / 1 已审核（D-20） */
    private Integer auditStatus;

    private String auditBy;

    private LocalDateTime auditTime;

    /** 0 草稿 / 1 已发布 / 2 已下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
