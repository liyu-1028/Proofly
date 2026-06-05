package com.lyllink.proofly.dto.resp;

public record OrderStatusResponse(
        String orderNo,
        String status,
        Boolean isPaid
) {}
