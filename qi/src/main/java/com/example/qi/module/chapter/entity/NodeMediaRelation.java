package com.example.qi.module.chapter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 节点图片引用（图文/工序节点等的多图支持），对应表 node_media_relation。
 */
@Data
@TableName("node_media_relation")
public class NodeMediaRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nodeId;

    private Long mediaId;

    /** 图片说明（C.7：图片说明字段） */
    private String caption;

    private Integer sortOrder;
}
