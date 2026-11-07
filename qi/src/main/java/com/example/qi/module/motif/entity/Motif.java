package com.example.qi.module.motif.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 纹饰资料（ADM-07），对应表 motif。
 * 发布条件：名称、文化解释、资料来源（5.2）；没有可靠依据的寓意不写（C.5）。
 */
@Data
@TableName("motif")
public class Motif {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String imageUrl;

    private String culturalExplain;

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
