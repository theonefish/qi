package com.example.qi.module.chapter.controller;

import com.example.qi.common.Result;
import com.example.qi.module.chapter.dto.NextTargetRequest;
import com.example.qi.module.chapter.dto.NodeSaveRequest;
import com.example.qi.module.chapter.entity.ChapterNode;
import com.example.qi.module.chapter.service.ChapterFlowService;
import com.example.qi.module.chapter.service.ChapterNodeService;
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

import java.util.List;

/**
 * 叙事节点管理（7.3）：新增、编辑、删除草稿、上下移动排序、详情。
 */
@RestController
@RequestMapping("/admin/chapters/{chapterId}/nodes")
@RequiredArgsConstructor
public class AdminChapterNodeController {

    private final ChapterNodeService chapterNodeService;
    private final ChapterFlowService chapterFlowService;

    /** 节点基础列表（按配置顺序，不含扩展数据） */
    @GetMapping
    public Result<List<ChapterNode>> list(@PathVariable Long chapterId) {
        return Result.ok(chapterNodeService.listByChapter(chapterId));
    }

    /** 节点详情（含类型扩展数据） */
    @GetMapping("/{nodeId}")
    public Result<ChapterNodeService.NodeDetail> detail(@PathVariable Long chapterId,
                                                        @PathVariable Long nodeId) {
        return Result.ok(chapterNodeService.detail(nodeId));
    }

    @PostMapping
    public Result<Long> save(@PathVariable Long chapterId, @Valid @RequestBody NodeSaveRequest request) {
        return Result.ok(chapterNodeService.save(chapterId, request));
    }

    @PutMapping("/{nodeId}")
    public Result<Long> update(@PathVariable Long chapterId, @PathVariable Long nodeId,
                               @Valid @RequestBody NodeSaveRequest request) {
        request.setId(nodeId);
        return Result.ok(chapterNodeService.save(chapterId, request));
    }

    @DeleteMapping("/{nodeId}")
    public Result<Void> delete(@PathVariable Long chapterId, @PathVariable Long nodeId) {
        chapterNodeService.delete(nodeId);
        return Result.ok();
    }

    /** 上下移动排序：direction = up / down */
    @PostMapping("/{nodeId}/move")
    public Result<Void> move(@PathVariable Long chapterId, @PathVariable Long nodeId,
                             @RequestParam String direction) {
        chapterNodeService.move(chapterId, nodeId, direction);
        return Result.ok();
    }

    /** 流向覆盖连线轻接口（分支叙事 §9.6）：targetNodeId 为空表示清除覆盖、回落默认链 */
    @PutMapping("/{nodeId}/next")
    public Result<Void> setNext(@PathVariable Long chapterId, @PathVariable Long nodeId,
                                @RequestBody NextTargetRequest request) {
        chapterFlowService.setNext(chapterId, nodeId, request.getTargetNodeId());
        return Result.ok();
    }
}
