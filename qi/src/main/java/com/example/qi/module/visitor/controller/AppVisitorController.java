package com.example.qi.module.visitor.controller;

import com.example.qi.common.Result;
import com.example.qi.module.visitor.dto.BehaviorReportRequest;
import com.example.qi.module.visitor.dto.CompleteChapterRequest;
import com.example.qi.module.visitor.dto.FavoriteRequest;
import com.example.qi.module.visitor.dto.ReadPositionRequest;
import com.example.qi.module.visitor.service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序访客接口（FR-01/05/09、ADM-11）：匿名访问，匿名标识由前端本地生成后随请求传递。
 * 匿名标识约定：小程序首次启动生成 UUID 存本地 Storage，后续所有 /app/visitor 请求携带。
 */
@RestController
@RequestMapping("/app/visitor")
@RequiredArgsConstructor
public class AppVisitorController {

    private final VisitorService visitorService;

    // ---------------------------- 收藏（FR-09） ----------------------------

    @PostMapping("/favorites")
    public Result<Void> addFavorite(@Valid @RequestBody FavoriteRequest request) {
        visitorService.addFavorite(request.getAnonymousId(), request.getTargetType(), request.getTargetId());
        return Result.ok();
    }

    @DeleteMapping("/favorites")
    public Result<Void> removeFavorite(@RequestParam String anonymousId,
                                       @RequestParam String targetType,
                                       @RequestParam Long targetId) {
        visitorService.removeFavorite(anonymousId, targetType, targetId);
        return Result.ok();
    }

    @GetMapping("/favorites")
    public Result<List<VisitorService.FavoriteItem>> listFavorites(@RequestParam String anonymousId) {
        return Result.ok(visitorService.listFavorites(anonymousId));
    }

    /** 藏品详情页收藏按钮状态 */
    @GetMapping("/favorites/exists")
    public Result<Boolean> isFavorite(@RequestParam String anonymousId,
                                      @RequestParam String targetType,
                                      @RequestParam Long targetId) {
        return Result.ok(visitorService.isFavorite(anonymousId, targetType, targetId));
    }

    // ---------------------------- 进度与断点续读（FR-05） ----------------------------

    /** 浏览到节点时上报阅读位置（离开章节前由前端在节点切换时调用；choicePath 为分支路径，可选） */
    @PostMapping("/progress/read")
    public Result<Void> updateReadPosition(@Valid @RequestBody ReadPositionRequest request) {
        visitorService.updateReadPosition(request.getAnonymousId(), request.getChapterId(),
                request.getNodeId(), request.getChoicePath());
        return Result.ok();
    }

    /** 完成本章（幂等）：返回是否首次完成与最新进度，供完成反馈展示 */
    @PostMapping("/progress/complete")
    public Result<VisitorService.CompleteResult> completeChapter(@Valid @RequestBody CompleteChapterRequest request) {
        return Result.ok(visitorService.completeChapter(request.getAnonymousId(), request.getChapterId()));
    }

    /** 参观进度页 / 云展馆章节状态 */
    @GetMapping("/progress")
    public Result<List<VisitorService.ChapterProgressItem>> progress(@RequestParam String anonymousId) {
        return Result.ok(visitorService.listProgress(anonymousId));
    }

    /** 首页汇总：进度 N/M、收藏数、继续参观定位 */
    @GetMapping("/summary")
    public Result<VisitorService.VisitorSummary> summary(@RequestParam String anonymousId) {
        return Result.ok(visitorService.summary(anonymousId));
    }

    // ---------------------------- 行为上报（ADM-11） ----------------------------

    /**
     * 行为事件上报（支持批量）。前端调用时机：
     * 进入章节详情 → enter_chapter；节点首次进入视口 → view_node；
     * 点击关联卡 → click_relation（带 targetType/targetId）；点击回到主线 → back_main（带来源章节）。
     * complete_chapter 由 /progress/complete 内部记录，前端无需上报。
     */
    @PostMapping("/behaviors")
    public Result<Integer> report(@Valid @RequestBody BehaviorReportRequest request) {
        return Result.ok(visitorService.reportBehaviors(
                request.getAnonymousId(), request.getVisitId(), request.getEvents()));
    }
}
