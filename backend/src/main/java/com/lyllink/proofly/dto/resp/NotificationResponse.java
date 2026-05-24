package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private Long receiverUserId;
    private Long projectId;
    private String type;
    private String title;
    private String content;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
