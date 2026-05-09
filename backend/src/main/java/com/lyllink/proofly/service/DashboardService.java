package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lyllink.proofly.dao.AuditLogMapper;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.dto.resp.AuditLogResponse;
import com.lyllink.proofly.dto.resp.DashboardStatsResponse;
import com.lyllink.proofly.dto.resp.ProjectResponse;
import com.lyllink.proofly.entity.AuditLogEntity;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.entity.UserEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    public DashboardService(
            ProjectMapper projectMapper,
            AuditLogMapper auditLogMapper,
            UserMapper userMapper
    ) {
        this.projectMapper = projectMapper;
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
    }

    public DashboardStatsResponse getStats(CurrentUser currentUser) {
        Long storeId = currentUser.storeId();

        DashboardStatsResponse resp = new DashboardStatsResponse();

        // 1. Project counts by status
        List<ProjectEntity> allProjects = projectMapper.selectList(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStoreId, storeId)
                .eq(ProjectEntity::getDeleted, false));
        
        Map<String, Long> statusCounts = allProjects.stream()
                .collect(Collectors.groupingBy(ProjectEntity::getStatus, Collectors.counting()));
        resp.setStatusCounts(statusCounts);
        resp.setTotalProjects((long) allProjects.size());

        // 2. Recent projects (top 5)
        List<ProjectEntity> recentProjectEntities = projectMapper.selectList(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStoreId, storeId)
                .eq(ProjectEntity::getDeleted, false)
                .orderByDesc(ProjectEntity::getCreatedAt)
                .last("LIMIT 5"));

        // Fetch nicknames for mapping
        Set<Long> userIds = recentProjectEntities.stream()
                .flatMap(p -> Stream.of(p.getOwnerUserId(), p.getCreatedBy()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. Recent activities (top 10)
        List<AuditLogEntity> recentAuditEntities = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getStoreId, storeId)
                .orderByDesc(AuditLogEntity::getCreatedAt)
                .last("LIMIT 10"));
        
        Set<Long> auditUserIds = recentAuditEntities.stream()
                .filter(a -> "user".equals(a.getOperatorType()))
                .map(AuditLogEntity::getOperatorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        
        userIds.addAll(auditUserIds);

        Map<Long, String> nicknames = Map.of();
        if (!userIds.isEmpty()) {
            nicknames = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                    .in(UserEntity::getId, userIds)
                    .select(UserEntity::getId, UserEntity::getNickname))
                    .stream()
                    .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
        }

        Map<Long, String> finalNicknames = nicknames;
        resp.setRecentProjects(recentProjectEntities.stream()
                .map(p -> toProjectResponse(p, finalNicknames))
                .collect(Collectors.toList()));
        
        resp.setRecentActivities(recentAuditEntities.stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList()));

        return resp;
    }

    private ProjectResponse toProjectResponse(ProjectEntity p, Map<Long, String> nicknames) {
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

    private AuditLogResponse toAuditLogResponse(AuditLogEntity a) {
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(a.getId());
        resp.setAction(a.getAction());
        resp.setTargetType(a.getTargetType());
        resp.setTargetId(a.getTargetId());
        resp.setOperatorType(a.getOperatorType());
        resp.setOperatorName(a.getOperatorName());
        resp.setSummary(a.getSummary());
        resp.setCreatedAt(a.getCreatedAt());
        return resp;
    }
}
