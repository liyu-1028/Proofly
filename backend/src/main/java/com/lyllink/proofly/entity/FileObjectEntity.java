package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("file_object")
public class FileObjectEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long projectId;
    private Long versionId;
    private String objectKey;
    private String bucket;
    private String originalFilename;
    private String safeFilename;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String sha256;
    private String fileRole;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
