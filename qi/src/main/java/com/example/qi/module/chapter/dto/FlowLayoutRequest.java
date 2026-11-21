package com.example.qi.module.chapter.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 图编排画布坐标批量保存请求（分支叙事设计稿 §9.6，拖动防抖调用）。
 */
@Data
public class FlowLayoutRequest {

    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "节点 id 不能为空")
        private Long nodeId;

        /** 画布坐标（x,y） */
        private String flowPos;
    }
}
