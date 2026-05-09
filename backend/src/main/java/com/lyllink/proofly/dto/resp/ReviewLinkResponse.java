package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewLinkResponse {
    private Long id;
    private Long projectId;
    private Long currentVersionId;
    private String token; // Only populated when created or refreshed
    private String status;
    private LocalDateTime expiresAt;
    private Integer maxAccessCount;
    private Integer accessCount;
    private LocalDateTime lastAccessAt;
    private String url;
    private LocalDateTime createdAt;
}
