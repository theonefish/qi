package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分支抉择选项（分支叙事），对应表 node_choice。
 * 随所属 BRANCH 节点整体保存（先删后插），物理删除无逻辑删。
 */
@Data
@TableName("node_choice")
public class NodeChoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 BRANCH 节点 */
    private Long nodeId;

    /** 按钮文案，如「去制胎坊」 */
    private String label;

    /** 选中后到达的同章节点 */
    private Long targetNodeId;

    /** 按钮排序 */
    private Integer sortOrder;
}
