package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long userId,
        Long storeId,
        String username,
        String nickname,
        String phone,
        String email,
        String status,
        List<String> roles,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
