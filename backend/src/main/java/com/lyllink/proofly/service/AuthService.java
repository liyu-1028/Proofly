package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.RoleMapper;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.dto.req.LoginRequest;
import com.lyllink.proofly.dto.req.RefreshTokenRequest;
import com.lyllink.proofly.dto.resp.AuthResponse;
import com.lyllink.proofly.dto.resp.MeResponse;
import com.lyllink.proofly.entity.StoreEntity;
import com.lyllink.proofly.entity.UserEntity;
import com.lyllink.proofly.security.CurrentUser;
import com.lyllink.proofly.security.TokenClaims;
import com.lyllink.proofly.security.TokenPair;
import com.lyllink.proofly.security.TokenType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.dao.UserRoleMapper;
import com.lyllink.proofly.dto.req.RegisterRequest;
import com.lyllink.proofly.entity.RoleEntity;
import com.lyllink.proofly.entity.UserRoleEntity;
import java.util.UUID;

@Service
public class AuthService {

    private static final String ACTIVE = "active";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final StoreMapper storeMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ReferralService referralService;

    public AuthService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            StoreMapper storeMapper,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            ReferralService referralService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.storeMapper = storeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.referralService = referralService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        // 1. 检查手机号是否已注册
        if (userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getPhone, request.getPhone()).eq(UserEntity::getDeleted, false)) > 0) {
            throw BusinessException.conflict("该手机号已注册");
        }

        // 2. 创建门店
        StoreEntity store = new StoreEntity();
        long storeId = IdWorker.getId();
        store.setId(storeId);
        store.setName(request.getStoreName().trim());
        store.setStatus(ACTIVE);
        store.setDeploymentMode("multi-tenant");
        store.setPlanType("free");
        store.setInviteCode(generateInviteCode());
        store.setCreatedAt(LocalDateTime.now());
        store.setUpdatedAt(LocalDateTime.now());
        store.setDeleted(false);
        storeMapper.insert(store);

        // 第2步：如果存在邀请码，绑定推荐关系
        if (StringUtils.hasText(request.getInviteCode())) {
            referralService.bindReferral(storeId, request.getInviteCode().trim());
        }

        // 3. 创建用户 (所有者)
        UserEntity user = new UserEntity();
        long userId = IdWorker.getId();
        user.setId(userId);
        user.setStoreId(storeId);
        user.setUsername(request.getPhone()); // 自助注册使用手机号作为用户名
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(false);
        userMapper.insert(user);

        // 4. 初始化新门店的默认角色
        initializeStoreRoles(storeId, userId);
    }

    private void initializeStoreRoles(Long storeId, Long ownerId) {
        RoleEntity ownerRole = createRole(storeId, "owner", "门店老板", "管理门店项目和员工");
        RoleEntity designerRole = createRole(storeId, "designer", "设计师", "创建项目、上传版本、处理标注");
        
        // 为用户分配所有者角色
        UserRoleEntity ur = new UserRoleEntity();
        ur.setId(IdWorker.getId());
        ur.setStoreId(storeId);
        ur.setUserId(ownerId);
        ur.setRoleId(ownerRole.getId());
        ur.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(ur);
    }

    private RoleEntity createRole(Long storeId, String code, String name, String description) {
        RoleEntity role = new RoleEntity();
        role.setId(IdWorker.getId());
        role.setStoreId(storeId);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setDeleted(false);
        roleMapper.insert(role);
        return role;
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
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
