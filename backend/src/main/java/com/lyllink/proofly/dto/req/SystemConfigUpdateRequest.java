package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigUpdateRequest {
    @NotBlank(message = "配置值不能为空")
    private String configValue;
    private String description;
}
