package com.example.qi.module.inheritor.controller;

import com.example.qi.common.Result;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.service.InheritorService;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.relation.dto.RelationSaveRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 传承人管理（ADM-06）。
 */
@RestController
@RequestMapping("/admin/inheritors")
@RequiredArgsConstructor
public class AdminInheritorController {

    private final InheritorService inheritorService;

    public record PublishBody(@NotBlank(message = "修改内容说明不能为空") String modifySummary) {
    }

    public record AuditBody(@NotNull(message = "审核结论不能为空") Boolean pass) {
    }

    @GetMapping
    public Result<List<Inheritor>> list() {
        return Result.ok(inheritorService.list());
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody InheritorService.InheritorSave request) {
        return Result.ok(inheritorService.save(null, request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody InheritorService.InheritorSave request) {
        return Result.ok(inheritorService.save(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inheritorService.delete(id);
        return Result.ok();
    }

    /** 简单自审（D-20） */
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditBody request) {
        inheritorService.audit(id, Boolean.TRUE.equals(request.pass()));
        return Result.ok();
    }

    /** 发布（5.2 校验 + 发布记录） */
    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody PublishBody request) {
        return Result.ok(inheritorService.publish(id, request.modifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        inheritorService.offShelf(id);
        return Result.ok();
    }

    /** 传承人关联管理（8.2：代表作品/擅长技艺） */
    @PutMapping("/{id}/relations")
    public Result<Void> setRelations(@PathVariable Long id, @Valid @RequestBody RelationSaveRequest request) {
        inheritorService.setRelations(id, request);
        return Result.ok();
    }
}
