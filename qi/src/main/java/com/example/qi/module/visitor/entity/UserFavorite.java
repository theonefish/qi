package com.example.qi.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 匿名用户收藏（FR-09；D-05 服务端按匿名标识存储），对应表 user_favorite。
 * P0 仅藏品（D-16），targetType 字段为将来扩展保留。
 */
@Data
@TableName("user_favorite")
public class UserFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String anonymousId;

    private String targetType;

    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
