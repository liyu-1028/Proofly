package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("`user`")
public class UserEntity {

    @TableId
    private Long id;
    private Long storeId;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String passwordHash;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
