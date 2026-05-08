package com.lyllink.proofly.auth;

import java.time.OffsetDateTime;

public record TokenPair(
        String accessToken,
        String accessTokenId,
        OffsetDateTime accessTokenExpiresAt,
        String refreshToken,
        String refreshTokenId,
        OffsetDateTime refreshTokenExpiresAt
) {
}
