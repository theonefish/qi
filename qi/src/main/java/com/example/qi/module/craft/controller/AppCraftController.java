package com.example.qi.module.craft.controller;

import com.example.qi.common.Result;
import com.example.qi.module.craft.entity.Craft;
import com.example.qi.module.craft.service.CraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序端工艺接口（FR-10）：匿名访问，仅已发布内容。
 */
@RestController
@RequestMapping("/app/crafts")
@RequiredArgsConstructor
public class AppCraftController {

    private final CraftService craftService;

    /** 工艺总览列表 */
    @GetMapping
    public Result<List<Craft>> list() {
        return Result.ok(craftService.appList());
    }

    /** 工艺详情：工序步骤 + 关联代表性藏品（仅有效目标） */
    @GetMapping("/{id}")
    public Result<CraftService.AppCraftDetail> detail(@PathVariable Long id) {
        return Result.ok(craftService.appDetail(id));
    }
}
