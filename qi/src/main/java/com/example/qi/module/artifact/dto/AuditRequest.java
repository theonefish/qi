package com.example.qi.module.artifact.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 简单自审请求（D-20）：通过 / 不通过。
 */
@Data
public class AuditRequest {

    @NotNull(message = "审核结论不能为空")
    private Boolean pass;
}
