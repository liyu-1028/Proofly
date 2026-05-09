package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectUpdateRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    private String customerName;

    private String customerContact;

    @NotNull(message = "项目负责人不能为空")
    private Long ownerUserId;

    private String remark;
}
