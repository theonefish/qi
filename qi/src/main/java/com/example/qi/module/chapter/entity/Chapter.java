package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节（ADM-03），对应表 chapter。
 * D-02：类型仅序章/正章，数量不写死，前台展示全部已发布章节。
 */
@Data
@TableName("chapter")
public class Chapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String subtitle;

    private String coverUrl;

    private String summary;

    /** 0 序章 / 1 正章（D-02） */
    private Integer chapterType;

    /** 展示顺序（5.5：按配置排序） */
    private Integer sortOrder;

    /** 0 草稿 / 1 已发布 / 2 已下架（5.3） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
