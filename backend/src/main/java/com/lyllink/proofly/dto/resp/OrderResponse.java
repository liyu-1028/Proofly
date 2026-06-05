package com.lyllink.proofly.dto.resp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long storeId,
        String orderNo,
        String planType,
        BigDecimal amount,
        Integer durationMonths,
        String status,
        String paymentMethod,
        String payUrl,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {}
