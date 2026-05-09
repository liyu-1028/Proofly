package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditLogResponse {
    private Long id;
    private String action;
    private String targetType;
    private Long targetId;
    private String operatorType;
    private String operatorName;
    private String summary;
    private LocalDateTime createdAt;
}
