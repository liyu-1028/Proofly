package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("referral_record")
public class ReferralRecordEntity {

    @TableId
    private Long id;
    private Long inviterStoreId;
    private Long inviterUserId;
    private Long inviteeStoreId;
    private String status; // pending, rewarded
    private LocalDateTime rewardedAt;
    private Long triggerProjectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
