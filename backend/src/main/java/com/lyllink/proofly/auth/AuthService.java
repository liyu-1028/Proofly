package com.lyllink.proofly.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.store.StoreEntity;
import com.lyllink.proofly.store.StoreMapper;
import com.lyllink.proofly.user.RoleMapper;
import com.lyllink.proofly.user.UserEntity;
import com.lyllink.proofly.user.UserMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final String ACTIVE = "active";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final StoreMapper storeMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            StoreMapper storeMapper,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.storeMapper = storeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = findLoginUser(request.account());
        validateUserAndStore(user);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("账号或密码错误");
        }

        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getStoreId(), user.getId());
        CurrentUser currentUser = toCurrentUser(user, roles, null);
        TokenPair tokenPair = tokenService.createTokenPair(currentUser);

        UserEntity update = new UserEntity();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);

        AuthResponse.UserSummary userSummary = toUserSummary(user, roles);
        tokenService.cacheUserProfile(userSummary);
        return new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt(),
                userSummary
        );
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        TokenClaims refreshClaims = tokenService.parseAndValidate(request.refreshToken(), TokenType.REFRESH);
        UserEntity user = userMapper.selectById(refreshClaims.userId());
        validateUserAndStore(user);
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getStoreId(), user.getId());
        CurrentUser currentUser = toCurrentUser(user, roles, null);
        String newAccessToken = tokenService.createAccessToken(currentUser, OffsetDateTime.now());
        TokenClaims accessClaims = tokenService.parseAndValidate(newAccessToken, TokenType.ACCESS);

        AuthResponse.UserSummary userSummary = toUserSummary(user, roles);
        tokenService.cacheUserProfile(userSummary);
        return new AuthResponse(
                newAccessToken,
                request.refreshToken(),
                accessClaims.expiresAt(),
                refreshClaims.expiresAt(),
                userSummary
        );
    }

    public void logout(String accessToken) {
        TokenClaims claims = tokenService.parseAndValidate(accessToken, TokenType.ACCESS);
        tokenService.deleteAccessToken(claims.tokenId());
        tokenService.blacklist(claims.tokenId(), claims.expiresAt(), "logout");
    }

    public MeResponse me(CurrentUser currentUser) {
        UserEntity user = userMapper.selectById(currentUser.userId());
        validateUserAndStore(user);
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getStoreId(), user.getId());
        return new MeResponse(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getStatus(),
                roles
        );
    }

    private UserEntity findLoginUser(String account) {
        if (!StringUtils.hasText(account)) {
            throw BusinessException.unauthorized("账号或密码错误");
        }
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getDeleted, false)
                .and(query -> query
                        .eq(UserEntity::getUsername, account)
                        .or()
                        .eq(UserEntity::getPhone, account))
                .last("LIMIT 1");
        UserEntity user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw BusinessException.unauthorized("账号或密码错误");
        }
        return user;
    }

    private void validateUserAndStore(UserEntity user) {
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw BusinessException.unauthorized("账号不存在");
        }
        if (!ACTIVE.equals(user.getStatus())) {
            throw BusinessException.forbidden("账号已停用或锁定");
        }
        StoreEntity store = storeMapper.selectById(user.getStoreId());
        if (store == null || Boolean.TRUE.equals(store.getDeleted()) || !ACTIVE.equals(store.getStatus())) {
            throw BusinessException.forbidden("门店已停用");
        }
    }

    private CurrentUser toCurrentUser(UserEntity user, List<String> roles, String tokenId) {
        return new CurrentUser(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getNickname(),
                List.copyOf(roles),
                tokenId
        );
    }

    private AuthResponse.UserSummary toUserSummary(UserEntity user, List<String> roles) {
        return new AuthResponse.UserSummary(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getStatus(),
                List.copyOf(roles)
        );
    }
}
