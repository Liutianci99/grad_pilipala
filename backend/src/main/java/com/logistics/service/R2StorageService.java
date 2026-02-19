package com.logistics.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cloudflare R2存储服务接口
 */
public interface R2StorageService {
    
    /**
     * 上传文件到R2
     * @param file 文件
     * @return 文件的公共访问URL
     */
    String uploadFile(MultipartFile file);
}
