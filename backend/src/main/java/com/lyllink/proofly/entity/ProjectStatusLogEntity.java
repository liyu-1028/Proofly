package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("project_status_log")
public class ProjectStatusLogEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long projectId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String operatorType;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
}
