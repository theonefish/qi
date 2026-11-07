package com.example.qi.module.artifact.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.BusinessException;
import com.example.qi.common.ContentStatus;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.entity.ArtifactCategory;
import com.example.qi.module.artifact.mapper.ArtifactCategoryMapper;
import com.example.qi.module.artifact.mapper.ArtifactMapper;
import com.example.qi.module.artifact.service.ArtifactService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 小程序端藏品接口（FR-07 / FR-08）：匿名访问，仅返回已发布内容（5.3）。
 */
@RestController
@RequestMapping("/app/artifacts")
@RequiredArgsConstructor
public class AppArtifactController {

    private final ArtifactMapper artifactMapper;
    private final ArtifactCategoryMapper categoryMapper;
    private final ArtifactService artifactService;

    /** 藏品列表：默认全部已发布，支持分类筛选，按“最新添加”排序（FR-07） */
    @GetMapping
    public Result<PageResult<Artifact>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long categoryId) {
        Page<Artifact> page = artifactMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Artifact>lambdaQuery()
                        .eq(Artifact::getStatus, ContentStatus.PUBLISHED.getCode())
                        .eq(categoryId != null, Artifact::getCategoryId, categoryId)
                        .orderByDesc(Artifact::getCreateTime));
        return Result.ok(PageResult.of(page));
    }

    /** 藏品详情：类别名称 + 有效关联卡，草稿和已下架内容不可见（AC-04） */
    @GetMapping("/{id}")
    public Result<ArtifactService.AppArtifactDetail> detail(@PathVariable Long id) {
        return Result.ok(artifactService.appDetail(id));
    }

    /** 分类筛选项：仅展示实际存在已发布藏品的类别（FR-07，不为凑分类虚构条目） */
    @GetMapping("/categories")
    public Result<List<ArtifactCategory>> categories() {
        // DISTINCT category_id 常量列名，非外部输入拼接
        List<Object> usedIds = artifactMapper.selectObjs(new QueryWrapper<Artifact>()
                .select("DISTINCT category_id")
                .eq("status", ContentStatus.PUBLISHED.getCode()));
        Set<Long> ids = new HashSet<>();
        for (Object id : usedIds) {
            if (id instanceof Long l) {
                ids.add(l);
            }
        }
        List<ArtifactCategory> categories = categoryMapper.selectList(
                Wrappers.<ArtifactCategory>lambdaQuery().orderByAsc(ArtifactCategory::getSortOrder));
        return Result.ok(categories.stream().filter(c -> ids.contains(c.getId())).toList());
    }
}
