package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectVersionResponse {
    private Long id;
    private Long storeId;
    private Long projectId;
    private Integer versionNo;
    private String versionName;
    private Long fileId;
    private String originalFilename;
    private String fileExt;
    private Long fileSize;
    private String previewUrl;
    private String description;
    private Boolean isCurrent;
    private Boolean isConfirmed;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private Long uploadedBy;
    private String uploaderNickname;
    private Integer annotationCount;
    private Boolean hasVoice;
}
