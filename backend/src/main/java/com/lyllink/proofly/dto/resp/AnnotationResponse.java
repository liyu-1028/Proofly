package com.lyllink.proofly.dto.resp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnotationResponse {
    private Long id;
    private String type;
    private BigDecimal xRatio;
    private BigDecimal yRatio;
    private BigDecimal widthRatio;
    private BigDecimal heightRatio;
    private String content;
    private String customerName;
    private String status;
    private LocalDateTime createdAt;
    private String resolvedByNickname;
    private LocalDateTime resolvedAt;
}
