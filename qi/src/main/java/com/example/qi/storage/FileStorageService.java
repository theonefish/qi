package com.example.qi.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象（9.1 部署与存储口径）：
 * 本期为本地磁盘实现，将来上线时可替换为 MinIO / 云对象存储实现，业务代码不动。
 */
public interface FileStorageService {

    /**
     * 保存文件，返回可直接访问的 URL（形如 /files/2026/11/uuid.jpg）。
     */
    String store(MultipartFile file);

    /**
     * 按 store 返回的 URL 删除文件；URL 非法或文件不存在时静默返回。
     */
    void deleteByUrl(String url);
}
