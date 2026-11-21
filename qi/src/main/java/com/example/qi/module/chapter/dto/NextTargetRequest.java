package com.example.qi.module.chapter.dto;

import lombok.Data;

/**
 * 流向覆盖连线请求（分支叙事设计稿 §9.6）。
 * targetNodeId 为空表示清除覆盖边（回落 sortOrder 默认链）。
 */
@Data
public class NextTargetRequest {

    /** 目标节点 id（同章其他节点），null=清除覆盖 */
    private Long targetNodeId;
}
