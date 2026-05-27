package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("annotation")
public class AnnotationEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long projectId;
    private Long versionId;
    private Long reviewLinkId;
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
    private String status; // open, resolved, ignored
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
