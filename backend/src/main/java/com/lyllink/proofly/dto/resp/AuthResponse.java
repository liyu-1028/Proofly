package com.lyllink.proofly.dto.resp;

import java.time.OffsetDateTime;
import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt,
        UserSummary user
) {

    public record UserSummary(
            Long userId,
            Long storeId,
            String username,
            String nickname,
            String phone,
            String status,
            List<String> roles
    ) {
    }
}
