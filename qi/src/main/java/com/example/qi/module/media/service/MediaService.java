package com.example.qi.module.media.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.module.media.entity.MediaAsset;
import com.example.qi.module.media.mapper.MediaAssetMapper;
import com.example.qi.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 媒体资源服务（ADM-08）：上传保存元数据、分页查询。
 * TODO 被引用资源删除保护（7.8）在章节/纹饰模块完成后补全引用检查。
 */
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "wav", "m4a");
    private static final Set<String> VIDEO_EXT = Set.of("mp4");

    private final MediaAssetMapper mediaAssetMapper;
    private final FileStorageService fileStorageService;

    public MediaAsset upload(MultipartFile file, String name, String source, String authorizationNote) {
        String url = fileStorageService.store(file);
        MediaAsset asset = new MediaAsset();
        asset.setName(name == null || name.isBlank() ? extractFileName(file) : name);
        asset.setUrl(url);
        asset.setMediaType(guessType(url));
        asset.setSource(source);
        asset.setAuthorizationNote(authorizationNote);
        asset.setFileSize(file.getSize());
        mediaAssetMapper.insert(asset);
        return asset;
    }

    public Page<MediaAsset> page(long pageNum, long pageSize, String mediaType, String keyword) {
        return mediaAssetMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<MediaAsset>lambdaQuery()
                        .eq(mediaType != null && !mediaType.isBlank(), MediaAsset::getMediaType, mediaType)
                        .like(keyword != null && !keyword.isBlank(), MediaAsset::getName, keyword)
                        .orderByDesc(MediaAsset::getCreateTime));
    }

    public MediaAsset getById(Long id) {
        return mediaAssetMapper.selectById(id);
    }

    private String extractFileName(MultipartFile file) {
        String original = file.getOriginalFilename();
        return original == null || original.isBlank() ? "未命名资源" : original;
    }

    private String guessType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        String ext = dot < 0 ? "" : lower.substring(dot + 1);
        if (IMAGE_EXT.contains(ext)) {
            return "image";
        }
        if (AUDIO_EXT.contains(ext)) {
            return "audio";
        }
        if (VIDEO_EXT.contains(ext)) {
            return "video";
        }
        return "file";
    }
}
