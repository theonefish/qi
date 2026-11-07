package com.example.qi.module.motif.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.motif.entity.Motif;
import com.example.qi.module.motif.service.MotifService;
import com.example.qi.module.publish.entity.PublishRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * 纹饰资料管理（ADM-07）。
 */
@RestController
@RequestMapping("/admin/motifs")
@RequiredArgsConstructor
public class AdminMotifController {

    private final MotifService motifService;

    public record PublishBody(@NotBlank(message = "修改内容说明不能为空") String modifySummary) {
    }

    @GetMapping
    public Result<PageResult<Motif>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        Page<Motif> page = motifService.page(pageNum, pageSize, keyword);
        return Result.ok(PageResult.of(page));
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody MotifService.MotifSave request) {
        return Result.ok(motifService.save(null, request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody MotifService.MotifSave request) {
        return Result.ok(motifService.save(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        motifService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody PublishBody request) {
        return Result.ok(motifService.publish(id, request.modifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        motifService.offShelf(id);
        return Result.ok();
    }
}
