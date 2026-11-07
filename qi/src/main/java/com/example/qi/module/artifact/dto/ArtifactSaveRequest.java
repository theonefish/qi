package com.example.qi.module.artifact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 藏品新增/编辑请求（5.2 最低发布条件对应字段前置校验）。
 */
@Data
public class ArtifactSaveRequest {

    /** 为空表示新增，编辑时必传 */
    private Long id;

    @NotNull(message = "类别不能为空")
    private Long categoryId;

    @NotBlank(message = "名称不能为空")
    private String name;

    private String era;

    private String material;

    private String craftDesc;

    private String story;

    private String motifMeaning;

    @NotBlank(message = "主展示图不能为空")
    private String mainImageUrl;

    @NotBlank(message = "资料来源不能为空")
    private String source;

    private String authorizationNote;
}
