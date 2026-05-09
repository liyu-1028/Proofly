package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectResponse {
    private Long id;
    private Long storeId;
    private String name;
    private String customerName;
    private String customerContact;
    private Long ownerUserId;
    private String ownerNickname;
    private String status;
    private Long currentVersionId;
    private Long confirmedVersionId;
    private String remark;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private String createdByNickname;
    private LocalDateTime updatedAt;
}
