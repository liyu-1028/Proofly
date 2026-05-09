package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("review_link")
public class ReviewLinkEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long projectId;
    private Long currentVersionId;
    private String tokenHash;
    private String status;
    private LocalDateTime expiresAt;
    private Integer maxAccessCount;
    private Integer accessCount;
    private LocalDateTime lastAccessAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
