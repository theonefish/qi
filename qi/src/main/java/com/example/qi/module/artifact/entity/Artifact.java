package com.example.qi.module.artifact.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 藏品档案（ADM-04），对应表 artifact。
 * 状态/审核字段口径见 5.3 与 D-20。
 */
@Data
@TableName("artifact")
public class Artifact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String era;

    private String material;

    private String craftDesc;

    private String story;

    private String motifMeaning;

    private String mainImageUrl;

    private String source;

    private String authorizationNote;

    /** 0 未审核 / 1 已审核（D-20） */
    private Integer auditStatus;

    /** 审核人自动取当前管理员 */
    private String auditBy;

    private LocalDateTime auditTime;

    /** 0 草稿 / 1 已发布 / 2 已下架（5.3） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
