package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 时间轴节点与历史事件引用（node_type = TIMELINE），对应表 node_event_relation。
 */
@Data
@TableName("node_event_relation")
public class NodeEventRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nodeId;

    private Long eventId;

    /** 节点内展示顺序 */
    private Integer sortOrder;
}
