package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.RoleMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.dao.UserRoleMapper;
import com.lyllink.proofly.dto.req.UserCreateRequest;
import com.lyllink.proofly.dto.req.UserResetPasswordRequest;
import com.lyllink.proofly.dto.req.UserStatusUpdateRequest;
import com.lyllink.proofly.dto.req.UserUpdateRequest;
import com.lyllink.proofly.dto.resp.UserResponse;
import com.lyllink.proofly.entity.RoleEntity;
import com.lyllink.proofly.entity.UserEntity;
import com.lyllink.proofly.entity.UserRoleEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private static final String ACTIVE = "active";
    private static final String DISABLED = "disabled";
    private static final String LOCKED = "locked";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> list(CurrentUser currentUser, String keyword, String status) {
        requireOwnerOrAdmin(currentUser);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getStoreId, currentUser.storeId())
                .eq(UserEntity::getDeleted, false)
                .orderByDesc(UserEntity::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserEntity::getStatus, status.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(query -> query
                    .like(UserEntity::getUsername, like)
                    .or()
                    .like(UserEntity::getNickname, like)
                    .or()
                    .like(UserEntity::getPhone, like));
        }
        return userMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(CurrentUser currentUser, UserCreateRequest request) {
        requireOwnerOrAdmin(currentUser);
        String username = request.username().trim();
        String phone = trimToNull(request.phone());
        ensureUsernameAvailable(currentUser.storeId(), username, null);
        ensurePhoneAvailable(currentUser.storeId(), phone, null);

        UserEntity user = new UserEntity();
        user.setId(IdWorker.getId());
        user.setStoreId(currentUser.storeId());
        user.setUsername(username);
        user.setNickname(request.nickname().trim());
        user.setPhone(phone);
        user.setEmail(trimToNull(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(ACTIVE);
        user.setCreatedBy(currentUser.userId());
        user.setUpdatedBy(currentUser.userId());
        user.setDeleted(false);
        userMapper.insert(user);

        replaceRoles(currentUser, user.getId(), normalizeRoleCodes(request.roleCodes()));
        return toResponse(requiredUser(currentUser.storeId(), user.getId()));
    }

    public UserResponse detail(CurrentUser currentUser, Long userId) {
        requireOwnerOrAdmin(currentUser);
        return toResponse(requiredUser(currentUser.storeId(), userId));
    }

    @Transactional
    public UserResponse update(CurrentUser currentUser, Long userId, UserUpdateRequest request) {
        requireOwnerOrAdmin(currentUser);
        UserEntity user = requiredUser(currentUser.storeId(), userId);
        String phone = trimToNull(request.phone());
        ensurePhoneAvailable(currentUser.storeId(), phone, userId);

        user.setNickname(request.nickname().trim());
        user.setPhone(phone);
        user.setEmail(trimToNull(request.email()));
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        if (request.roleCodes() != null) {
            replaceRoles(currentUser, userId, normalizeRoleCodes(request.roleCodes()));
        }
        return toResponse(requiredUser(currentUser.storeId(), userId));
    }

    @Transactional
    public UserResponse updateStatus(CurrentUser currentUser, Long userId, UserStatusUpdateRequest request) {
        requireOwnerOrAdmin(currentUser);
        if (currentUser.userId().equals(userId)) {
            throw BusinessException.badRequest("不能修改当前登录账号状态");
        }
        String status = request.status().trim();
        if (!Set.of(ACTIVE, DISABLED, LOCKED).contains(status)) {
            throw BusinessException.badRequest("用户状态只能是 active、disabled 或 locked");
        }
        UserEntity user = requiredUser(currentUser.storeId(), userId);
        user.setStatus(status);
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toResponse(requiredUser(currentUser.storeId(), userId));
    }

    @Transactional
    public void resetPassword(CurrentUser currentUser, Long userId, UserResetPasswordRequest request) {
        requireOwnerOrAdmin(currentUser);
        UserEntity user = requiredUser(currentUser.storeId(), userId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUpdatedBy(currentUser.userId());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private UserEntity requiredUser(Long storeId, Long userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getStoreId, storeId)
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, false)
                .last("LIMIT 1"));
        if (user == null) {
            throw BusinessException.notFound("员工不存在");
        }
        return user;
    }

    private void replaceRoles(CurrentUser currentUser, Long userId, List<String> roleCodes) {
        List<RoleEntity> roles = selectRoles(currentUser.storeId(), roleCodes);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getStoreId, currentUser.storeId())
                .eq(UserRoleEntity::getUserId, userId));
        for (RoleEntity role : roles) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setId(IdWorker.getId());
            relation.setStoreId(currentUser.storeId());
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            relation.setCreatedBy(currentUser.userId());
            userRoleMapper.insert(relation);
        }
    }

    private List<RoleEntity> selectRoles(Long storeId, List<String> roleCodes) {
        List<RoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getDeleted, false)
                .in(RoleEntity::getCode, roleCodes)
                .and(query -> query.eq(RoleEntity::getStoreId, storeId).or().isNull(RoleEntity::getStoreId)));
        Map<String, RoleEntity> byCode = roles.stream()
                .collect(Collectors.toMap(RoleEntity::getCode, Function.identity(), (left, right) -> left));
        List<String> missing = roleCodes.stream()
                .filter(code -> !byCode.containsKey(code))
                .toList();
        if (!missing.isEmpty()) {
            throw BusinessException.badRequest("角色不存在：" + String.join("、", missing));
        }
        return roleCodes.stream().map(byCode::get).toList();
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        List<String> source = roleCodes == null || roleCodes.isEmpty() ? List.of("designer") : roleCodes;
        List<String> normalized = new ArrayList<>();
        for (String roleCode : source) {
            if (!StringUtils.hasText(roleCode)) {
                continue;
            }
            normalized.add(roleCode.trim());
        }
        if (normalized.isEmpty()) {
            throw BusinessException.badRequest("至少选择一个角色");
        }
        return List.copyOf(new LinkedHashSet<>(normalized));
    }

    private UserResponse toResponse(UserEntity user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getStoreId(), user.getId());
        return new UserResponse(
                user.getId(),
                user.getStoreId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                List.copyOf(roles),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private void ensureUsernameAvailable(Long storeId, String username, Long excludeUserId) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getStoreId, storeId)
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, false);
        if (excludeUserId != null) {
            wrapper.ne(UserEntity::getId, excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
    }

    private void ensurePhoneAvailable(Long storeId, String phone, Long excludeUserId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getStoreId, storeId)
                .eq(UserEntity::getPhone, phone)
                .eq(UserEntity::getDeleted, false);
        if (excludeUserId != null) {
            wrapper.ne(UserEntity::getId, excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("手机号已存在");
        }
    }

    private void requireOwnerOrAdmin(CurrentUser currentUser) {
        if (!currentUser.roles().contains("owner") && !currentUser.roles().contains("admin")) {
            throw BusinessException.forbidden("当前账号无员工管理权限");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
