package com.example.qi.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 匿名用户浏览记录（FR-12 P1），对应表 browse_history。
 * 同一匿名标识下按 (targetType, targetId) 去重，只保留最近一次浏览时间；
 * 与行为事件 behavior_event 分离，清空浏览记录不影响 ADM-11 统计数据。
 */
@Data
@TableName("browse_history")
public class BrowseHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String anonymousId;

    /** chapter / artifact / craft / inheritor */
    private String targetType;

    private Long targetId;

    /** 最近浏览时间（重复浏览只更新时间，不新增行） */
    private LocalDateTime visitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
