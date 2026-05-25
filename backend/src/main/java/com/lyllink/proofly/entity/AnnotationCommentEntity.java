package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("annotation_comment")
public class AnnotationCommentEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long annotationId;
    private Long projectId;
    private Long versionId;
    private String replyType; // user, customer, system
    private Long replyUserId;
    private String replyName;
    private String content;
    private String mediaUrl;
    private Integer mediaDuration;
    private LocalDateTime createdAt;
}
