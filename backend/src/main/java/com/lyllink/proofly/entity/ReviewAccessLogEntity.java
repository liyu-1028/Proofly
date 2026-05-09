package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("review_access_log")
public class ReviewAccessLogEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long reviewLinkId;
    private Long projectId;
    private Long versionId;
    private String ip;
    private String userAgent;
    private String referer;
    private LocalDateTime accessedAt;
}
