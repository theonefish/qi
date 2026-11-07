package com.example.qi.common;

import lombok.Getter;

/**
 * 内容生命周期状态（5.3）：草稿 / 已发布 / 已下架，对应数据库 status 字段。
 */
@Getter
public enum ContentStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFF_SHELF(2, "已下架");

    private final int code;
    private final String label;

    ContentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
