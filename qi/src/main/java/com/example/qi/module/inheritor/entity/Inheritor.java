package com.example.qi.module.inheritor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传承人档案（ADM-06），对应表 inheritor。
 * 本轮为最小 CRUD，发布流程与自审 UI 后续迭代补充。
 */
@Data
@TableName("inheritor")
public class Inheritor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String photoUrl;

    private String intro;

    private String career;

    private String story;

    private String source;

    /** 0 未审核 / 1 已审核（D-20，本轮暂不提供审核操作） */
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
