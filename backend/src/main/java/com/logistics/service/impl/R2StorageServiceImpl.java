package com.logistics.service.impl;

import com.logistics.service.R2StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

/**
 * Cloudflare R2存储服务实现类
 * 注意：需要在application.yml中配置R2相关信息
 */
@Slf4j
@Service
public class R2StorageServiceImpl implements R2StorageService {
    
    // TODO: 待配置 - 在application.yml中添加以下配置项
    @Value("${cloudflare.r2.account-id:}")
    private String accountId;
    
    @Value("${cloudflare.r2.access-key-id:}")
    private String accessKeyId;
    
    @Value("${cloudflare.r2.secret-access-key:}")
    private String secretAccessKey;
    
    @Value("${cloudflare.r2.bucket-name:}")
    private String bucketName;
    
    @Value("${cloudflare.r2.public-url:}")
    private String publicUrl;
    
    private S3Client s3Client;
    
    /**
     * 初始化S3客户端（用于连接Cloudflare R2）
     */
    private S3Client getS3Client() {
        if (s3Client == null) {
            // Cloudflare R2的endpoint格式: https://{accountId}.r2.cloudflarestorage.com
            String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);
            
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .region(Region.of("auto"))
                    .build();
        }
        return s3Client;
    }
    
    @Override
    public String uploadFile(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("上传文件不能为空");
            }
            
            // 生成唯一文件名，存储在goods_image文件夹中
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
            String fileName = "goods_image/" + UUID.randomUUID().toString() + extension;
            
            // 上传到R2
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();
            
            getS3Client().putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            // 返回公共访问URL
            String fileUrl = publicUrl.endsWith("/") 
                    ? publicUrl + fileName 
                    : publicUrl + "/" + fileName;
            
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
}
