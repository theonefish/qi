package com.example.qi.module.culture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.culture.entity.TimelineEvent;
import com.example.qi.module.culture.service.TimelineEventService;
import com.example.qi.module.publish.entity.PublishRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史事件管理（ADM-07）：章节时间轴的数据源。
 */
@RestController
@RequestMapping("/admin/timeline-events")
@RequiredArgsConstructor
public class AdminTimelineEventController {

    private final TimelineEventService timelineEventService;

    public record PublishBody(@NotBlank(message = "修改内容说明不能为空") String modifySummary) {
    }

    @GetMapping
    public Result<PageResult<TimelineEvent>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        Page<TimelineEvent> page = timelineEventService.page(pageNum, pageSize, keyword);
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody TimelineEventService.EventSave request) {
        return Result.ok(timelineEventService.save(null, request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody TimelineEventService.EventSave request) {
        return Result.ok(timelineEventService.save(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        timelineEventService.delete(id);
        return Result.ok();
    }

    /** 发布（5.2 校验 + 发布记录） */
    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody PublishBody request) {
        return Result.ok(timelineEventService.publish(id, request.modifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        timelineEventService.offShelf(id);
        return Result.ok();
    }
}
