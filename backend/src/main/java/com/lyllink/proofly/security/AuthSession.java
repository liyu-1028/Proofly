package com.lyllink.proofly.security;

import java.time.OffsetDateTime;
import java.util.List;

public record AuthSession(
        Long userId,
        Long storeId,
        String username,
        String nickname,
        List<String> roles,
        String tokenId,
        String tokenType,
        OffsetDateTime loginAt,
        OffsetDateTime expiresAt
) {
}
