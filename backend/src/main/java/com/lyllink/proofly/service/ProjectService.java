package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.ProjectStatusLogMapper;
import com.lyllink.proofly.dao.RoleMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.dto.req.ProjectCreateRequest;
import com.lyllink.proofly.dto.req.ProjectUpdateRequest;
import com.lyllink.proofly.dto.resp.ProjectResponse;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.entity.ProjectStatusLogEntity;
import com.lyllink.proofly.entity.UserEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_WAITING_FEEDBACK = "waiting_feedback";
    public static final String STATUS_CHANGE_REQUESTED = "change_requested";
    public static final String STATUS_WAITING_CONFIRM = "waiting_confirm";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_ARCHIVED = "archived";

    private final ProjectMapper projectMapper;
    private final ProjectStatusLogMapper projectStatusLogMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UsageService usageService;

    public ProjectService(
            ProjectMapper projectMapper,
            ProjectStatusLogMapper projectStatusLogMapper,
            UserMapper userMapper,
            RoleMapper roleMapper,
            UsageService usageService
    ) {
        this.projectMapper = projectMapper;
        this.projectStatusLogMapper = projectStatusLogMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.usageService = usageService;
    }

    public List<ProjectResponse> list(CurrentUser currentUser, String keyword, String status, Long ownerUserId) {
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStoreId, currentUser.storeId())
                .eq(ProjectEntity::getDeleted, false)
                .orderByDesc(ProjectEntity::getCreatedAt);

        if (StringUtils.hasText(status)) {
            wrapper.eq(ProjectEntity::getStatus, status.trim());
        }
        if (ownerUserId != null) {
            wrapper.eq(ProjectEntity::getOwnerUserId, ownerUserId);
        }
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(query -> query
                    .like(ProjectEntity::getName, like)
                    .or()
                    .like(ProjectEntity::getCustomerName, like));
        }

        List<ProjectEntity> projects = projectMapper.selectList(wrapper);
        if (projects.isEmpty()) {
            return List.of();
        }

        // Fetch user nicknames for owners and creators
        Set<Long> userIds = projects.stream()
                .flatMap(p -> Stream.of(p.getOwnerUserId(), p.getCreatedBy()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nicknames = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getId, userIds)
                .select(UserEntity::getId, UserEntity::getNickname))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

        return projects.stream()
                .map(p -> toResponse(p, nicknames))
                .toList();
    }

    @Transactional
    public ProjectResponse create(CurrentUser currentUser, ProjectCreateRequest request) {
        usageService.checkProjectLimit(currentUser.storeId());
        ensureDesignerInStore(currentUser.storeId(), request.getOwnerUserId());

        ProjectEntity project = new ProjectEntity();
        project.setId(IdWorker.getId());
        project.setStoreId(currentUser.storeId());
        project.setName(request.getName().trim());
        project.setCustomerName(trimToNull(request.getCustomerName()));
        project.setCustomerContact(trimToNull(request.getCustomerContact()));
        project.setOwnerUserId(request.getOwnerUserId());
        project.setStatus(STATUS_DRAFT);
        project.setRemark(trimToNull(request.getRemark()));
        project.setCreatedBy(currentUser.userId());
        project.setUpdatedBy(currentUser.userId());
        project.setDeleted(false);
        projectMapper.insert(project);

        logStatusChange(currentUser, project, null, STATUS_DRAFT, "CREATE");

        return detail(currentUser, project.getId());
    }

    public ProjectResponse detail(CurrentUser currentUser, Long projectId) {
        return detailInternal(currentUser.storeId(), projectId);
    }

    public ProjectResponse detailInternal(Long storeId, Long projectId) {
        ProjectEntity project = requiredProject(storeId, projectId);
        
        Set<Long> userIds = Stream.of(project.getOwnerUserId(), project.getCreatedBy())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nicknames = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getId, userIds)
                .select(UserEntity::getId, UserEntity::getNickname))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        
        return toResponse(project, nicknames);
    }

    @Transactional
    public ProjectResponse update(CurrentUser currentUser, Long projectId, ProjectUpdateRequest request) {
        ProjectEntity project = requiredProject(currentUser.storeId(), projectId);
        
        // Cannot edit archived or confirmed projects (except for specific roles or status changes)
        if (STATUS_ARCHIVED.equals(project.getStatus())) {
            throw BusinessException.badRequest("已归档的项目不能编辑");
        }

        ensureDesignerInStore(currentUser.storeId(), request.getOwnerUserId());

        project.setName(request.getName().trim());
        project.setCustomerName(trimToNull(request.getCustomerName()));
        project.setCustomerContact(trimToNull(request.getCustomerContact()));
        project.setOwnerUserId(request.getOwnerUserId());
        project.setRemark(trimToNull(request.getRemark()));
        project.setUpdatedBy(currentUser.userId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        return detail(currentUser, projectId);
    }

    @Transactional
    public ProjectResponse archive(CurrentUser currentUser, Long projectId) {
        ProjectEntity project = requiredProject(currentUser.storeId(), projectId);
        if (STATUS_ARCHIVED.equals(project.getStatus())) {
            return detail(currentUser, projectId);
        }

        String fromStatus = project.getStatus();
        project.setStatus(STATUS_ARCHIVED);
        project.setArchivedAt(LocalDateTime.now());
        project.setUpdatedBy(currentUser.userId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        logStatusChange(currentUser, project, fromStatus, STATUS_ARCHIVED, "ARCHIVE");

        return detail(currentUser, projectId);
    }

    @Transactional
    public ProjectResponse restore(CurrentUser currentUser, Long projectId) {
        ProjectEntity project = requiredProject(currentUser.storeId(), projectId);
        if (!STATUS_ARCHIVED.equals(project.getStatus())) {
            return detail(currentUser, projectId);
        }

        // Restore to confirmed if it was confirmed, otherwise draft or waiting_feedback?
        // Let's simplify: restore to draft if no versions, or waiting_feedback if has versions.
        // Actually, for MVP, restoring simply clears archived status. 
        // We should ideally track the previous status. But let's just go back to draft for now if not confirmed.
        String toStatus = project.getConfirmedVersionId() != null ? STATUS_CONFIRMED : STATUS_DRAFT;
        
        String fromStatus = project.getStatus();
        project.setStatus(toStatus);
        project.setArchivedAt(null);
        project.setUpdatedBy(currentUser.userId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        logStatusChange(currentUser, project, fromStatus, toStatus, "RESTORE");

        return detail(currentUser, projectId);
    }

    private ProjectEntity requiredProject(Long storeId, Long projectId) {
        ProjectEntity project = projectMapper.selectOne(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStoreId, storeId)
                .eq(ProjectEntity::getId, projectId)
                .eq(ProjectEntity::getDeleted, false)
                .last("LIMIT 1"));
        if (project == null) {
            throw BusinessException.notFound("项目不存在");
        }
        return project;
    }

    private void ensureDesignerInStore(Long storeId, Long userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getStoreId, storeId)
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, false)
                .eq(UserEntity::getStatus, "active")
                .select(UserEntity::getId)
                .last("LIMIT 1"));
        if (user == null) {
            throw BusinessException.badRequest("负责人不存在、不属于该门店或已停用");
        }
        List<String> roles = roleMapper.selectRoleCodesByUserId(storeId, userId);
        if (!roles.contains("designer")) {
            throw BusinessException.badRequest("项目负责人必须是设计师");
        }
    }

    private void logStatusChange(CurrentUser currentUser, ProjectEntity project, String fromStatus, String toStatus, String action) {
        ProjectStatusLogEntity log = new ProjectStatusLogEntity();
        log.setId(IdWorker.getId());
        log.setStoreId(project.getStoreId());
        log.setProjectId(project.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorType("user");
        log.setOperatorId(currentUser.userId());
        log.setOperatorName(currentUser.nickname());
        log.setCreatedAt(LocalDateTime.now());
        projectStatusLogMapper.insert(log);
    }

    private ProjectResponse toResponse(ProjectEntity p, Map<Long, String> nicknames) {
        ProjectResponse resp = new ProjectResponse();
        resp.setId(p.getId());
        resp.setStoreId(p.getStoreId());
        resp.setName(p.getName());
        resp.setCustomerName(p.getCustomerName());
        resp.setCustomerContact(p.getCustomerContact());
        resp.setOwnerUserId(p.getOwnerUserId());
        resp.setOwnerNickname(nicknames.getOrDefault(p.getOwnerUserId(), "未知"));
        resp.setStatus(p.getStatus());
        resp.setCurrentVersionId(p.getCurrentVersionId());
        resp.setConfirmedVersionId(p.getConfirmedVersionId());
        resp.setRemark(p.getRemark());
        resp.setArchivedAt(p.getArchivedAt());
        resp.setCreatedAt(p.getCreatedAt());
        resp.setCreatedByNickname(nicknames.getOrDefault(p.getCreatedBy(), "未知"));
        resp.setUpdatedAt(p.getUpdatedAt());
        return resp;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
