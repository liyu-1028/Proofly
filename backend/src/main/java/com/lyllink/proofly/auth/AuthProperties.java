package com.lyllink.proofly.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "proofly.auth")
public record AuthProperties(
        @NotBlank String jwtSecret,
        @Min(1) long accessTokenTtlMinutes,
        @Min(1) long refreshTokenTtlDays,
        @NotBlank String redisPrefix
) {
}
