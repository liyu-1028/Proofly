package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("confirmation_record")
public class ConfirmationRecordEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long projectId;
    private Long versionId;
    private Long reviewLinkId;
    private String customerName;
    private String customerContact;
    private LocalDateTime confirmedAt;
    private String ip;
    private String userAgent;
    private String status; // valid, voided
    private LocalDateTime voidedAt;
    private String voidReason;
    private LocalDateTime createdAt;
}
