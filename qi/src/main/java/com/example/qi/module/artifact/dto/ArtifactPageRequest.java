package com.example.qi.module.artifact.dto;

import lombok.Data;

/**
 * 藏品分页查询请求（7.12：列表页统一查询、分页、状态筛选）。
 */
@Data
public class ArtifactPageRequest {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 名称模糊查询 */
    private String name;

    private Long categoryId;

    /** 0 草稿 / 1 已发布 / 2 已下架，空则查全部 */
    private Integer status;
}
