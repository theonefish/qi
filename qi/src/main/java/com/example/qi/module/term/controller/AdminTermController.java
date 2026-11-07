package com.example.qi.module.term.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.term.entity.Term;
import com.example.qi.module.term.service.TermService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

/**
 * 术语管理（ADM-07 / FR-06）。
 */
@RestController
@RequestMapping("/admin/terms")
@RequiredArgsConstructor
public class AdminTermController {

    private final TermService termService;

    public record PublishBody(@NotBlank(message = "修改内容说明不能为空") String modifySummary) {
    }

    public record AuditBody(@NotNull(message = "审核结论不能为空") Boolean pass) {
    }

    @GetMapping
    public Result<PageResult<Term>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        Page<Term> page = termService.page(pageNum, pageSize, keyword);
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody TermService.TermSave request) {
        return Result.ok(termService.save(null, request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody TermService.TermSave request) {
        return Result.ok(termService.save(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        termService.delete(id);
        return Result.ok();
    }

    /** 简单自审（D-20） */
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditBody request) {
        termService.audit(id, Boolean.TRUE.equals(request.pass()));
        return Result.ok();
    }

    /** 发布（5.2：名称、释义、审核记录） */
    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody PublishBody request) {
        return Result.ok(termService.publish(id, request.modifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        termService.offShelf(id);
        return Result.ok();
    }
}
