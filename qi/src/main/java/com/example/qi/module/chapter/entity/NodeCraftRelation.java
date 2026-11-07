package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 工序节点与工艺/工序引用（node_type = PROCESS），对应表 node_craft_relation。
 * 引用已有工艺和工序数据，不复制维护（7.3）。
 */
@Data
@TableName("node_craft_relation")
public class NodeCraftRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nodeId;

    private Long craftId;

    /** 引用的工序，可空（空表示整工艺概览） */
    private Long craftStepId;

    private Integer sortOrder;
}
