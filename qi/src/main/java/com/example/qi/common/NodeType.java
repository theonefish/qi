package com.example.qi.common;

import lombok.Getter;

/**
 * 叙事节点类型（6.13）：节点类型对应数据库 chapter_node.node_type。
 * BRANCH 为分支抉择节点（分支叙事）：content 存引导文案，选项存 node_choice，强制选择不参与线性链。
 */
@Getter
public enum NodeType {

    TEXT("TEXT", "图文"),
    TIMELINE("TIMELINE", "时间轴"),
    QUOTE("QUOTE", "人物口述"),
    PROCESS("PROCESS", "工序步骤"),
    BRANCH("BRANCH", "分支抉择");

    private final String code;
    private final String label;

    NodeType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
