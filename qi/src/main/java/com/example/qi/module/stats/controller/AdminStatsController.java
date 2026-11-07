package com.example.qi.module.stats.controller;

import com.example.qi.common.Result;
import com.example.qi.module.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 参观行为统计（ADM-11）与数据总览（ADM-02）素材接口。
 */
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    /** 数据总览卡片：内容数量 + 访客数 + 今日事件数 */
    @GetMapping("/overview")
    public Result<StatsService.Overview> overview() {
        return Result.ok(statsService.overview());
    }

    /** 章节维度：进入、节点浏览、完成、回到主线次数 */
    @GetMapping("/chapters")
    public Result<List<StatsService.ChapterStat>> chapters() {
        return Result.ok(statsService.chapterStats());
    }

    /** 关联跳转 Top 50：按来源章节与目标内容 */
    @GetMapping("/relations")
    public Result<List<StatsService.RelationStat>> relations() {
        return Result.ok(statsService.relationStats());
    }
}
