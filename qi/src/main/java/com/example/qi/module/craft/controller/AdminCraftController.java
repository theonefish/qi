package com.example.qi.module.craft.controller;

import com.example.qi.common.Result;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.entity.CraftStep;
import com.example.qi.module.craft.service.CraftService;
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

import java.util.List;

/**
 * 工艺与工序管理（ADM-05）。
 */
@RestController
@RequestMapping("/admin/crafts")
@RequiredArgsConstructor
public class AdminCraftController {

    private final CraftService craftService;

    public record PublishBody(@NotBlank(message = "修改内容说明不能为空") String modifySummary) {
    }

    @GetMapping
    public Result<List<Craft>> list() {
        return Result.ok(craftService.list());
    }

    @PostMapping
    public Result<Long> save(@Valid @RequestBody CraftService.CraftSave request) {
        return Result.ok(craftService.saveCraft(null, request));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody CraftService.CraftSave request) {
        return Result.ok(craftService.saveCraft(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        craftService.deleteCraft(id);
        return Result.ok();
    }

    /** 发布（5.2 校验 + 发布记录） */
    @PostMapping("/{id}/publish")
    public Result<PublishRecord> publish(@PathVariable Long id, @Valid @RequestBody PublishBody request) {
        return Result.ok(craftService.publish(id, request.modifySummary()));
    }

    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        craftService.offShelf(id);
        return Result.ok();
    }

    @GetMapping("/{id}/steps")
    public Result<List<CraftStep>> steps(@PathVariable Long id) {
        return Result.ok(craftService.listSteps(id));
    }

    @PostMapping("/{id}/steps")
    public Result<Long> saveStep(@PathVariable Long id, @Valid @RequestBody CraftService.StepSave request) {
        return Result.ok(craftService.saveStep(id, null, request));
    }

    @PutMapping("/{id}/steps/{stepId}")
    public Result<Long> updateStep(@PathVariable Long id, @PathVariable Long stepId,
                                   @Valid @RequestBody CraftService.StepSave request) {
        return Result.ok(craftService.saveStep(id, stepId, request));
    }

    @DeleteMapping("/{id}/steps/{stepId}")
    public Result<Void> deleteStep(@PathVariable Long id, @PathVariable Long stepId) {
        craftService.deleteStep(id, stepId);
        return Result.ok();
    }

    @PostMapping("/{id}/steps/{stepId}/move")
    public Result<Void> moveStep(@PathVariable Long id, @PathVariable Long stepId,
                                 @RequestParam String direction) {
        craftService.moveStep(id, stepId, direction);
        return Result.ok();
    }
}
