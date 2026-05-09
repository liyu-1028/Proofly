package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("audit_log")
public class AuditLogEntity {

    @TableId
    private Long id;
    private Long storeId;
    private String action;
    private String targetType;
    private Long targetId;
    private String operatorType; // user, customer, system
    private Long operatorId;
    private String operatorName;
    private String summary;
    private String extraJson;
    private LocalDateTime createdAt;
}
