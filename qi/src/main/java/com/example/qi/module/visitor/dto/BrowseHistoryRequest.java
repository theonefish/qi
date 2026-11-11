package com.example.qi.module.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 浏览记录上报（FR-12 P1）：进入章节 / 藏品 / 工艺 / 传承人详情页时调用。
 * 同一对象重复浏览只刷新浏览时间，不重复堆积记录。
 */
@Data
public class BrowseHistoryRequest {

    @NotBlank(message = "缺少匿名标识")
    private String anonymousId;

    /** chapter / artifact / craft / inheritor */
    @NotBlank(message = "缺少浏览对象类型")
    private String targetType;

    @NotNull(message = "缺少浏览对象 ID")
    private Long targetId;
}
