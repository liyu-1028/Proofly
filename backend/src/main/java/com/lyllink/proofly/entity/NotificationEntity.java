package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("notification")
public class NotificationEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long receiverUserId;
    private Long projectId;
    private String type;
    private String title;
    private String content;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
