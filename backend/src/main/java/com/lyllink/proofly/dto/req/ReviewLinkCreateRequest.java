package com.lyllink.proofly.dto.req;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewLinkCreateRequest {
    private LocalDateTime expiresAt;
    private Integer maxAccessCount;
}
