package com.example.qi.module.relation.controller;

import com.example.qi.common.Result;
import com.example.qi.module.relation.entity.ContentRelation;
import com.example.qi.module.relation.service.RelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内容关联查询（ADM-09）：正向关联与反向关联查看。
 * 关联的建立/保存在各内容模块（如 PUT /admin/crafts/{id}/relations）。
 */
@RestController
@RequestMapping("/admin/relations")
@RequiredArgsConstructor
public class AdminRelationController {

    private final RelationService relationService;

    /** 正向：某内容建立的关联列表 */
    @GetMapping
    public Result<List<ContentRelation>> listBySource(@RequestParam String sourceType,
                                                      @RequestParam Long sourceId) {
        return Result.ok(relationService.listBySource(sourceType, sourceId));
    }

    /** 反向：某内容被哪些内容引用（7.9） */
    @GetMapping("/targets")
    public Result<List<ContentRelation>> listByTarget(@RequestParam String targetType,
                                                      @RequestParam Long targetId) {
        return Result.ok(relationService.listByTarget(targetType, targetId));
    }
}
