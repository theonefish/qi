package com.example.qi.module.craft.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工艺档案（ADM-05），对应表 craft。
 * 本轮为最小 CRUD，发布流程与自审后续迭代补充。
 */
@Data
@TableName("craft")
public class Craft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String overview;

    private String feature;

    /** 适用范围（工序因器而异说明，FR-10） */
    private String scope;

    private String source;

    /** 0 草稿 / 1 已发布 / 2 已下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
