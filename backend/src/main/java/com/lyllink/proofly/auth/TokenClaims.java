package com.lyllink.proofly.auth;

import java.time.OffsetDateTime;
import java.util.List;

public record TokenClaims(
        String tokenId,
        TokenType tokenType,
        Long userId,
        Long storeId,
        String username,
        String nickname,
        List<String> roles,
        OffsetDateTime expiresAt
) {
}
