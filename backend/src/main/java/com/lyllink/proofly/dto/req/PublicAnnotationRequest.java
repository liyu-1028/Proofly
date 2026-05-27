package com.lyllink.proofly.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicAnnotationRequest {
    private String type; // point, rect, text
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
    private String customerContact;
}
