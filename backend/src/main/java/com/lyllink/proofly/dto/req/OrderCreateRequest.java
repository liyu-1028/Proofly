package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateRequest {

    @NotNull(message = "购买时长（月）不能为空")
    private Integer durationMonths;

    private String paymentMethod; // wechat 或 alipay
}
