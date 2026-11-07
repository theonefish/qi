package com.example.qi.module.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员（ADM-01），对应表 admin_user。
 */
@Data
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希，禁止明文（7.1） */
    private String password;

    private String displayName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
