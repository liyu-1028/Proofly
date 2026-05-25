package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.config.MinioProperties;
import com.lyllink.proofly.dao.FileObjectMapper;
import com.lyllink.proofly.entity.FileObjectEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.io.InputStream;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileService {

    private final FileObjectMapper fileObjectMapper;
    private final StorageService storageService;
    private final MinioProperties minioProperties;

    public FileService(
            FileObjectMapper fileObjectMapper,
            StorageService storageService,
            MinioProperties minioProperties
    ) {
        this.fileObjectMapper = fileObjectMapper;
        this.storageService = storageService;
        this.minioProperties = minioProperties;
    }

    /**
     * 上传文件并保存元数据。
     */
    public FileObjectEntity uploadFile(
            CurrentUser currentUser,
            Long projectId,
            Long versionId,
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream,
            String fileRole
    ) {
        Long fileId = IdWorker.getId();
        String fileExt = StringUtils.getFilenameExtension(originalFilename);
        String objectKey = String.format("stores/%d/projects/%d/versions/%d/%d-%s",
                currentUser.storeId(), projectId, versionId, fileId, originalFilename);

        // 1. 上传到 MinIO
        storageService.putObject(objectKey, inputStream, contentType, size);

        // 2. 保存元数据到数据库
        FileObjectEntity entity = new FileObjectEntity();
        entity.setId(fileId);
        entity.setStoreId(currentUser.storeId());
        entity.setProjectId(projectId);
        entity.setVersionId(versionId);
        entity.setObjectKey(objectKey);
        entity.setBucket(minioProperties.getBucket());
        entity.setOriginalFilename(originalFilename);
        entity.setSafeFilename(originalFilename); // 可以进一步清理
        entity.setFileExt(fileExt);
        entity.setMimeType(contentType);
        entity.setFileSize(size);
        entity.setFileRole(fileRole);
        entity.setCreatedBy(currentUser.userId());
        entity.setUpdatedBy(currentUser.userId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(false);

        fileObjectMapper.insert(entity);
        return entity;
    }

    /**
     * 获取文件的预览 URL。
     */
    public String getFilePreviewUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return storageService.getPresignedUrl(objectKey);
    }

    /**
     * 上传文件并保存元数据（公开版本）。
     */
    public FileObjectEntity uploadPublicFile(
            Long storeId,
            Long projectId,
            Long versionId,
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream,
            String fileRole
    ) {
        Long fileId = IdWorker.getId();
        String fileExt = StringUtils.getFilenameExtension(originalFilename);
        String objectKey = String.format("stores/%d/public/%d-%s",
                storeId, fileId, originalFilename);

        // 1. 上传到 MinIO
        storageService.putObject(objectKey, inputStream, contentType, size);

        // 2. 保存元数据到数据库
        FileObjectEntity entity = new FileObjectEntity();
        entity.setId(fileId);
        entity.setStoreId(storeId);
        entity.setProjectId(projectId);
        entity.setVersionId(versionId);
        entity.setObjectKey(objectKey);
        entity.setBucket(minioProperties.getBucket());
        entity.setOriginalFilename(originalFilename);
        entity.setSafeFilename(originalFilename);
        entity.setFileExt(fileExt);
        entity.setMimeType(contentType);
        entity.setFileSize(size);
        entity.setFileRole(fileRole);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(false);

        fileObjectMapper.insert(entity);
        return entity;
    }
}
