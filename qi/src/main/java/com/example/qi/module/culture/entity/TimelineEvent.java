package com.example.qi.module.culture.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史事件（ADM-07），对应表 timeline_event，章节时间轴的数据源。
 * 年代不明时用时期表述，不得编造具体年份（C.3）。
 */
@Data
@TableName("timeline_event")
public class TimelineEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 时间或时期（自由表述，如"商周时期"） */
    private String eventTime;

    private String title;

    private String description;

    private Long mediaId;

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
