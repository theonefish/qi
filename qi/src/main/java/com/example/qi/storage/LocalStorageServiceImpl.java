package com.example.qi.storage;

import com.example.qi.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘存储实现（9.1）：文件按 yyyy/MM 目录存放，重命名为 UUID，
 * 扩展名走白名单（9.4 限制文件类型），不信任原始文件名。
 */
@Slf4j
@Service
public class LocalStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "mp3", "mp4", "wav", "m4a", "pdf");

    private static final String URL_PREFIX = "/files/";

    @Value("${file.storage-dir}")
    private String storageDir;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext);
        }
        String relative = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path target = Paths.get(storageDir, relative);
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("文件保存失败：{}", original, e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }
        return URL_PREFIX + relative;
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storageDir, url.substring(URL_PREFIX.length())));
        } catch (IOException e) {
            log.warn("文件删除失败：{}", url, e);
        }
    }
}
