package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;

public record StoreResponse(
        Long id,
        String name,
        String contactName,
        String contactPhone,
        String status,
        String deploymentMode,
        String planType,
        LocalDateTime planExpiresAt,
        String inviteCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
