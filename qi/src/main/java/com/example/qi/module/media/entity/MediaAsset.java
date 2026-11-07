package com.example.qi.module.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体资源元数据（ADM-08），对应表 media_asset。
 * 文件本体在对象存储/本地磁盘，数据库只存地址与元信息（4.1）。
 */
@Data
@TableName("media_asset")
public class MediaAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    /** image / audio / video / file */
    private String mediaType;

    private String caption;

    private String source;

    private String authorizationNote;

    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
