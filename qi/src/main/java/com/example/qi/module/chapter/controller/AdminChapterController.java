package com.example.qi.module.chapter.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.chapter.dto.ChapterPublishRequest;
import com.example.qi.module.chapter.dto.ChapterSaveRequest;
import com.example.qi.module.chapter.entity.Chapter;
import com.example.qi.module.chapter.service.ChapterService;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.service.PublishRecordService;
import com.example.qi.module.relation.dto.RelationSaveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chapters")
@RequiredArgsConstructor
public class AdminChapterController {

    private final ChapterService chapterService;
    private final PublishRecordService publishRecordService;

    @GetMapping
    public Result<PageResult<Chapter>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(chapterService.page(pageNum, pageSize, status, keyword)));
    }

    @GetMapping("/{id}")
    public Result<ChapterService.AdminChapterDetail> detail(@PathVariable Long id) {
        return Result.ok(chapterService.detail(id));
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody ChapterSaveRequest request) {
        return Result.ok(chapterService.save(request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody ChapterSaveRequest request) {
        request.setId(id);
        return Result.ok(chapterService.save(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        chapterService.delete(id);
        return Result.ok();
    }

    /** 章节关联（8.2：章节↔藏品/工艺/传承人） */
    @PutMapping("/{id}/relations")
    public Result<Void> setRelations(@PathVariable Long id, @Valid @RequestBody RelationSaveRequest request) {
        chapterService.setRelations(id, request);
        return Result.ok();
    }

    /** 发布（5.2 校验 + 7.14 发布记录） */
    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody ChapterPublishRequest request) {
        return Result.ok(chapterService.publish(id, request.getModifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        chapterService.offShelf(id);
        return Result.ok();
    }

    @GetMapping("/{id}/publish-records")
    public Result<?> publishRecords(@PathVariable Long id) {
        return Result.ok(publishRecordService.listByObject("chapter", id));
    }
}
