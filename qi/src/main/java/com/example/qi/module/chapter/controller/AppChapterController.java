package com.example.qi.module.chapter.controller;

import com.example.qi.common.Result;
import com.example.qi.module.chapter.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端章节接口（FR-02 云展馆 / FR-03 章节详情）：匿名访问，仅已发布内容。
 */
@RestController
@RequestMapping("/app/chapters")
@RequiredArgsConstructor
public class AppChapterController {

    private final ChapterService chapterService;

    /** 云展馆章节列表：按配置顺序展示全部已发布章节（D-02 数量不写死） */
    @GetMapping
    public Result<?> list() {
        return Result.ok(chapterService.appList());
    }

    /** 章节详情：章节 + 按顺序的叙事节点（含类型扩展数据）+ 有效关联卡 */
    @GetMapping("/{id}")
    public Result<ChapterService.AppChapterDetail> detail(@PathVariable Long id) {
        return Result.ok(chapterService.appDetail(id));
    }
}
