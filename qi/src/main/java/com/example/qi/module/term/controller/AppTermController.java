package com.example.qi.module.term.controller;

import com.example.qi.common.Result;
import com.example.qi.module.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序术语释义查询（FR-06 术语卡弹层）。
 * 正文中的 [[term:ID]] 轻量标记由前端解析出 ID 列表后，批量调用本接口取释义。
 * 仅返回已发布术语；未发布/不存在的 ID 不返回数据（前端不渲染点击标记）。
 */
@RestController
@RequestMapping("/app/terms")
@RequiredArgsConstructor
public class AppTermController {

    private final TermService termService;

    /** 批量查询：GET /app/terms?ids=1,2,3 */
    @GetMapping
    public Result<List<TermService.AppTerm>> listByIds(@RequestParam List<Long> ids) {
        return Result.ok(termService.appByIds(ids));
    }
}
