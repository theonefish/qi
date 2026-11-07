package com.example.qi.module.artifact.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发布请求（7.14：修改内容为必填项，不允许空说明）。
 */
@Data
public class PublishRequest {

    @NotBlank(message = "修改内容说明不能为空")
    private String modifySummary;
}
