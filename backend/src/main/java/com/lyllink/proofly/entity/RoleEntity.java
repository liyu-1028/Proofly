package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("role")
public class RoleEntity {

    @TableId
    private Long id;
    private Long storeId;
    private String code;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
