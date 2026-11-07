package com.example.qi.module.artifact.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.artifact.dto.AuditRequest;
import com.example.qi.module.artifact.dto.ArtifactPageRequest;
import com.example.qi.module.artifact.dto.ArtifactSaveRequest;
import com.example.qi.module.artifact.dto.PublishRequest;
import com.example.qi.module.artifact.entity.Artifact;
import com.example.qi.module.artifact.service.ArtifactService;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.relation.dto.RelationSaveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/artifacts")
@RequiredArgsConstructor
public class AdminArtifactController {

    private final ArtifactService artifactService;
    private final PublishRecordService publishRecordService;

    @GetMapping
    public Result<PageResult<Artifact>> page(ArtifactPageRequest request) {
        Page<Artifact> page = artifactService.page(request);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<ArtifactService.ArtifactDetail> detail(@PathVariable Long id) {
        return Result.ok(artifactService.getDetail(id));
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody ArtifactSaveRequest request) {
        return Result.ok(artifactService.save(request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody ArtifactSaveRequest request) {
        request.setId(id);
        return Result.ok(artifactService.save(request));
    }

    /** 保存关联列表（ADM-09） */
    @PutMapping("/{id}/relations")
    public Result<Void> setRelations(@PathVariable Long id, @Valid @RequestBody RelationSaveRequest request) {
        artifactService.setRelations(id, request);
        return Result.ok();
    }

    /** 简单自审（D-20） */
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        artifactService.audit(id, Boolean.TRUE.equals(request.getPass()));
        return Result.ok();
    }

    /** 发布（5.2 校验 + 7.14 发布记录） */
    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody PublishRequest request) {
        return Result.ok(artifactService.publish(id, request.getModifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        artifactService.offShelf(id);
        return Result.ok();
    }

    /** 该藏品的发布记录（仅查看，不提供回滚，D-23） */
    @GetMapping("/{id}/publish-records")
    public Result<?> publishRecords(@PathVariable Long id) {
        return Result.ok(publishRecordService.listByObject("artifact", id));
    }

    /** 内容发布记录统一查询（ADM-13） */
    @GetMapping("/publish-records")
    public Result<PageResult<PublishRecord>> publishRecordPage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(
                publishRecordService.pageContentRecords(pageNum, pageSize, objectType, keyword)));
    }
}
