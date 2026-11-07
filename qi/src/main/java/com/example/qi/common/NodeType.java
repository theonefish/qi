package com.example.qi.common;

import lombok.Getter;

/**
 * 叙事节点类型（6.13）：节点类型限定为四种，对应数据库 chapter_node.node_type。
 */
@Getter
public enum NodeType {

    TEXT("TEXT", "图文"),
    TIMELINE("TIMELINE", "时间轴"),
    QUOTE("QUOTE", "人物口述"),
    PROCESS("PROCESS", "工序步骤");

    private final String code;
    private final String label;

    NodeType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
