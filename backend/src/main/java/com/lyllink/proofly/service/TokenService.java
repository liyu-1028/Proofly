package com.lyllink.proofly.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.config.AuthProperties;
import com.lyllink.proofly.dto.resp.AuthResponse;
import com.lyllink.proofly.security.AuthSession;
import com.lyllink.proofly.security.CurrentUser;
import com.lyllink.proofly.security.TokenClaims;
import com.lyllink.proofly.security.TokenPair;
import com.lyllink.proofly.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final AuthProperties authProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final SecretKey secretKey;

    public TokenService(
            AuthProperties authProperties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.secretKey = Keys.hmacShaKeyFor(authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair createTokenPair(CurrentUser currentUser) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime accessExpiresAt = now.plusMinutes(authProperties.accessTokenTtlMinutes());
        OffsetDateTime refreshExpiresAt = now.plusDays(authProperties.refreshTokenTtlDays());
        String accessTokenId = newTokenId();
        String refreshTokenId = newTokenId();

        String accessToken = createToken(currentUser, accessTokenId, TokenType.ACCESS, now, accessExpiresAt);
        String refreshToken = createToken(currentUser, refreshTokenId, TokenType.REFRESH, now, refreshExpiresAt);

        writeSession(accessKey(accessTokenId), currentUser, accessTokenId, TokenType.ACCESS, now, accessExpiresAt);
        writeSession(refreshKey(refreshTokenId), currentUser, refreshTokenId, TokenType.REFRESH, now, refreshExpiresAt);

        return new TokenPair(accessToken, accessTokenId, accessExpiresAt, refreshToken, refreshTokenId, refreshExpiresAt);
    }

    public String createAccessToken(CurrentUser currentUser, OffsetDateTime loginAt) {
        String accessTokenId = newTokenId();
        OffsetDateTime accessExpiresAt = OffsetDateTime.now().plusMinutes(authProperties.accessTokenTtlMinutes());
        String accessToken = createToken(currentUser, accessTokenId, TokenType.ACCESS, OffsetDateTime.now(), accessExpiresAt);
        writeSession(accessKey(accessTokenId), currentUser, accessTokenId, TokenType.ACCESS, loginAt, accessExpiresAt);
        return accessToken;
    }

    public TokenClaims parseAndValidate(String token, TokenType expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw BusinessException.unauthorized("Token 无效或已过期");
        }

        String tokenType = claims.get("tokenType", String.class);
        if (!expectedType.name().equals(tokenType)) {
            throw BusinessException.unauthorized("Token 类型错误");
        }

        String tokenId = claims.getId();
        String blacklistReason = redisTemplate.opsForValue().get(blacklistKey(tokenId));
        if (blacklistReason != null) {
            throw BusinessException.unauthorized("Token 已失效");
        }

        String sessionKey = expectedType == TokenType.ACCESS ? accessKey(tokenId) : refreshKey(tokenId);
        String sessionJson = redisTemplate.opsForValue().get(sessionKey);
        if (sessionJson == null) {
            throw BusinessException.unauthorized("登录状态已过期");
        }

        Long userId = toLong(claims.get("userId"));
        Long storeId = toLong(claims.get("storeId"));
        String username = claims.get("username", String.class);
        String nickname = claims.get("nickname", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new TokenClaims(
                tokenId,
                expectedType,
                userId,
                storeId,
                username,
                nickname,
                roles == null ? List.of() : roles,
                claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC)
        );
    }

    public void deleteAccessToken(String tokenId) {
        redisTemplate.delete(accessKey(tokenId));
    }

    public void blacklist(String tokenId, OffsetDateTime expiresAt, String reason) {
        Duration ttl = Duration.between(OffsetDateTime.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(blacklistKey(tokenId), reason, ttl);
    }

    public void cacheUserProfile(AuthResponse.UserSummary userSummary) {
        try {
            String value = objectMapper.writeValueAsString(userSummary);
            redisTemplate.opsForValue().set(userProfileKey(userSummary.userId()), value, Duration.ofMinutes(30));
            redisTemplate.opsForValue().set(userRolesKey(userSummary.userId()), objectMapper.writeValueAsString(userSummary.roles()), Duration.ofMinutes(30));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize user profile", exception);
        }
    }

    public String accessKey(String tokenId) {
        return key("auth:access:" + tokenId);
    }

    public String refreshKey(String tokenId) {
        return key("auth:refresh:" + tokenId);
    }

    public String blacklistKey(String tokenId) {
        return key("auth:blacklist:" + tokenId);
    }

    private String userProfileKey(Long userId) {
        return key("user:profile:" + userId);
    }

    private String userRolesKey(Long userId) {
        return key("user:roles:" + userId);
    }

    private String createToken(CurrentUser currentUser, String tokenId, TokenType tokenType, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        return Jwts.builder()
                .id(tokenId)
                .subject(String.valueOf(currentUser.userId()))
                .claim("tokenType", tokenType.name())
                .claim("userId", currentUser.userId())
                .claim("storeId", currentUser.storeId())
                .claim("username", currentUser.username())
                .claim("nickname", currentUser.nickname())
                .claim("roles", currentUser.roles())
                .issuedAt(Date.from(issuedAt.toInstant()))
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(secretKey)
                .compact();
    }

    private void writeSession(String key, CurrentUser currentUser, String tokenId, TokenType tokenType, OffsetDateTime loginAt, OffsetDateTime expiresAt) {
        AuthSession session = new AuthSession(
                currentUser.userId(),
                currentUser.storeId(),
                currentUser.username(),
                currentUser.nickname(),
                currentUser.roles(),
                tokenId,
                tokenType.name(),
                loginAt,
                expiresAt
        );
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(session), Duration.between(OffsetDateTime.now(), expiresAt));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize auth session", exception);
        }
    }

    private String key(String suffix) {
        return authProperties.redisPrefix() + ":" + activeEnv() + ":" + suffix;
    }

    private String activeEnv() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "dev" : profiles[0];
    }

    private String newTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw BusinessException.unauthorized("Token 内容错误");
    }
}
