package com.example.qi.module.inheritor.controller;

import com.example.qi.common.Result;
import com.example.qi.module.inheritor.entity.Inheritor;
import com.example.qi.module.inheritor.service.InheritorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端传承人接口（FR-11）：匿名访问，仅已发布内容。
 */
@RestController
@RequestMapping("/app/inheritors")
@RequiredArgsConstructor
public class AppInheritorController {

    private final InheritorService inheritorService;

    /** 传承人列表：姓名、照片、简介 */
    @GetMapping
    public Result<List<Inheritor>> list() {
        return Result.ok(inheritorService.appList());
    }

    /** 传承人详情：从艺经历、传承故事、代表作品、擅长技艺（仅有效目标） */
    @GetMapping("/{id}")
    public Result<InheritorService.AppInheritorDetail> detail(@PathVariable Long id) {
        return Result.ok(inheritorService.appDetail(id));
    }
}
