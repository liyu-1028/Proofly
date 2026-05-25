package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("store")
public class StoreEntity {

    @TableId
    private Long id;
    private String name;
    private String contactName;
    private String contactPhone;
    private String status;
    private String planType;
    private LocalDateTime planExpiresAt;
    private String inviteCode;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
