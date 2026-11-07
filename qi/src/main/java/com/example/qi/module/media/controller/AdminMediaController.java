package com.example.qi.module.media.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.common.PageResult;
import com.example.qi.common.Result;
import com.example.qi.module.media.entity.MediaAsset;
import com.example.qi.module.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaService mediaService;

    /** 上传（ADM-08）：文件本体走 FileStorageService，数据库只存元数据 */
    @PostMapping("/upload")
    public Result<MediaAsset> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) String source,
                                     @RequestParam(required = false) String authorizationNote) {
        return Result.ok(mediaService.upload(file, name, source, authorizationNote));
    }

    @GetMapping
    public Result<PageResult<MediaAsset>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) String keyword) {
        Page<MediaAsset> page = mediaService.page(pageNum, pageSize, mediaType, keyword);
        return Result.ok(PageResult.of(page));
    }
}
