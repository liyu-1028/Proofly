package com.lyllink.proofly.service;

import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class StorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public StorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * 上传对象到 MinIO。
     */
    public void putObject(String objectKey, InputStream inputStream, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1L)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw BusinessException.serverError("文件上传存储系统失败: " + e.getMessage());
        }
    }

    /**
     * 生成对象的预签名 URL（默认 1 小时过期）。
     */
    public String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw BusinessException.serverError("获取文件预览链接失败: " + e.getMessage());
        }
    }
}
