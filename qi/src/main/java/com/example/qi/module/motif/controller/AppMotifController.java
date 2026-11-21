package com.example.qi.module.motif.controller;

import com.example.qi.common.Result;
import com.example.qi.module.motif.service.MotifService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序纹饰资料列表（FR 补充：纹饰主题页）。
 * 仅返回已发布纹饰；无图条目前端以占位样式展示。
 */
@RestController
@RequestMapping("/app/motifs")
@RequiredArgsConstructor
public class AppMotifController {

    private final MotifService motifService;

    @GetMapping
    public Result<List<MotifService.AppMotif>> list() {
        return Result.ok(motifService.appList());
    }
}
