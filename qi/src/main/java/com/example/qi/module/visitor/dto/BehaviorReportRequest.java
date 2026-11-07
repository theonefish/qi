package com.example.qi.module.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 行为事件上报（ADM-11；事件口径见 7.11）。
 * visitId 为前端每次冷启动生成的会话标识，服务端用于 enter_chapter / view_node 会话内去重。
 * complete_chapter 由“完成本章”接口内部记录，前端无需上报。
 */
@Data
public class BehaviorReportRequest {

    @NotBlank(message = "缺少匿名标识")
    private String anonymousId;

    @NotBlank(message = "缺少访问会话标识 visitId")
    private String visitId;

    private List<Item> events;

    @Data
    public static class Item {

        /** enter_chapter / view_node / click_relation / back_main */
        private String eventType;

        private Long chapterId;

        private Long nodeId;

        /** click_relation：跳转目标 */
        private String targetType;

        private Long targetId;
    }
}
