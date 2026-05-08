package com.lyllink.proofly.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_role")
public class UserRoleEntity {

    @TableId
    private Long id;
    private Long storeId;
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;
    private Long createdBy;
}
