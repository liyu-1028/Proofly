package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigResponse {
    private Long id;
    private Long storeId;
    private String configKey;
    private String configValue;
    private String valueType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
