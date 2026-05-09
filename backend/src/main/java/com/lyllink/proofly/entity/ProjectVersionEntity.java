package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("project_version")
public class ProjectVersionEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long projectId;
    private Integer versionNo;
    private String versionName;
    private Long fileId;
    private Long uploadedBy;
    private String description;
    private Boolean isCurrent;
    private Boolean isConfirmed;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
