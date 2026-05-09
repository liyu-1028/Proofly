package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.ConfirmationRecordMapper;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.ProjectVersionMapper;
import com.lyllink.proofly.entity.ConfirmationRecordEntity;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.entity.ProjectVersionEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmationService {

    private final ConfirmationRecordMapper confirmationRecordMapper;
    private final ProjectMapper projectMapper;
    private final ProjectVersionMapper projectVersionMapper;
    private final AuditLogService auditLogService;

    public ConfirmationService(
            ConfirmationRecordMapper confirmationRecordMapper,
            ProjectMapper projectMapper,
            ProjectVersionMapper projectVersionMapper,
            AuditLogService auditLogService
    ) {
        this.confirmationRecordMapper = confirmationRecordMapper;
        this.projectMapper = projectMapper;
        this.projectVersionMapper = projectVersionMapper;
        this.auditLogService = auditLogService;
    }

    /**
     * Customer confirms a design version.
     */
    @Transactional
    public ConfirmationRecordEntity confirm(ConfirmationRecordEntity record) {
        // 1. Check idempotency: is this project already confirmed with a valid record?
        ConfirmationRecordEntity existing = confirmationRecordMapper.selectOne(new LambdaQueryWrapper<ConfirmationRecordEntity>()
                .eq(ConfirmationRecordEntity::getProjectId, record.getProjectId())
                .eq(ConfirmationRecordEntity::getStatus, "valid")
                .last("LIMIT 1"));
        if (existing != null) {
            // If already confirmed with the SAME version, return it (idempotent)
            if (existing.getVersionId().equals(record.getVersionId())) {
                return existing;
            }
            throw BusinessException.badRequest("该项目已有确认记录，无法重复确认不同版本");
        }

        ProjectEntity project = projectMapper.selectById(record.getProjectId());
        if (project == null || project.getDeleted()) {
            throw BusinessException.notFound("项目不存在");
        }

        ProjectVersionEntity version = projectVersionMapper.selectById(record.getVersionId());
        if (version == null || version.getDeleted() || !version.getProjectId().equals(record.getProjectId())) {
            throw BusinessException.notFound("要确认的版本不存在");
        }

        // 2. Save confirmation record
        record.setId(IdWorker.getId());
        record.setConfirmedAt(LocalDateTime.now());
        record.setStatus("valid");
        record.setCreatedAt(LocalDateTime.now());
        confirmationRecordMapper.insert(record);

        // 3. Update project status
        project.setStatus("confirmed");
        project.setConfirmedVersionId(version.getId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        // 4. Update version status
        version.setIsConfirmed(true);
        version.setConfirmedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        projectVersionMapper.updateById(version);

        auditLogService.logCustomerAction(
                record.getStoreId(),
                "CONFIRM_PROJECT",
                "project",
                record.getProjectId(),
                record.getCustomerName(),
                "确认定稿了版本: " + version.getVersionName()
        );

        return record;
    }

    /**
     * Get confirmation for a project.
     */
    public ConfirmationRecordEntity getConfirmation(Long storeId, Long projectId) {
        return confirmationRecordMapper.selectOne(new LambdaQueryWrapper<ConfirmationRecordEntity>()
                .eq(ConfirmationRecordEntity::getStoreId, storeId)
                .eq(ConfirmationRecordEntity::getProjectId, projectId)
                .eq(ConfirmationRecordEntity::getStatus, "valid")
                .orderByDesc(ConfirmationRecordEntity::getConfirmedAt)
                .last("LIMIT 1"));
    }
}
