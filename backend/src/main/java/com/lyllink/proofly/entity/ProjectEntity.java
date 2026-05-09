package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("project")
public class ProjectEntity {

    @TableId
    private Long id;
    private Long storeId;
    private String name;
    private String customerName;
    private String customerContact;
    private Long ownerUserId;
    private String status;
    private Long currentVersionId;
    private Long confirmedVersionId;
    private String remark;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
