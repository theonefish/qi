package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人物口述节点扩展（node_type = QUOTE），对应表 node_quote。
 * 口述须为经核实授权的真实口述（C.6/C.7），一个节点一条。
 */
@Data
@TableName("node_quote")
public class NodeQuote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nodeId;

    private Long inheritorId;

    private String quoteText;

    private String source;

    private String authorizationNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
