package com.lyllink.proofly.dto.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnotationResponse {
    private Long id;
    private String type;
    @JsonProperty("xRatio")
    private BigDecimal xRatio;
    @JsonProperty("yRatio")
    private BigDecimal yRatio;
    private BigDecimal widthRatio;
    private BigDecimal heightRatio;
    private String content;
    private String mediaUrl;
    private Integer mediaDuration;
    private String customerName;
    private String status;
    private LocalDateTime createdAt;
    private String resolvedByNickname;
    private LocalDateTime resolvedAt;
}
