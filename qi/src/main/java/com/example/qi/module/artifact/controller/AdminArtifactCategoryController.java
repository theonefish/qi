package com.example.qi.module.artifact.controller;

import com.example.qi.common.Result;
import com.example.qi.module.artifact.entity.ArtifactCategory;
import com.example.qi.module.artifact.mapper.ArtifactCategoryMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 藏品类别字典维护（D-27）。
 */
@RestController
@RequestMapping("/admin/artifact-categories")
@RequiredArgsConstructor
public class AdminArtifactCategoryController {

    private final ArtifactCategoryMapper categoryMapper;

    public record CategorySaveRequest(@NotBlank(message = "类别名称不能为空") String name) {
    }

    @GetMapping
    public Result<List<ArtifactCategory>> list() {
        return Result.ok(categoryMapper.selectList(Wrappers.<ArtifactCategory>lambdaQuery()
                .orderByAsc(ArtifactCategory::getSortOrder)
                .orderByAsc(ArtifactCategory::getId)));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategorySaveRequest request) {
        ArtifactCategory category = new ArtifactCategory();
        category.setName(request.name());
        category.setSortOrder(0);
        categoryMapper.insert(category);
        return Result.ok(category.getId());
    }
}
