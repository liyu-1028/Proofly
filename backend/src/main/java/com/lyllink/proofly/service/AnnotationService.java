package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.AnnotationCommentMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lyllink.proofly.dao.AnnotationMapper;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.entity.AnnotationCommentEntity;
import com.lyllink.proofly.entity.AnnotationEntity;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnotationService {

    private final AnnotationMapper annotationMapper;
    private final AnnotationCommentMapper annotationCommentMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    public AnnotationService(
            AnnotationMapper annotationMapper,
            AnnotationCommentMapper annotationCommentMapper,
            ProjectMapper projectMapper,
            AuditLogService auditLogService
    ) {
        this.annotationMapper = annotationMapper;
        this.annotationCommentMapper = annotationCommentMapper;
        this.projectMapper = projectMapper;
        this.auditLogService = auditLogService;
    }

    /**
     * Customer submits an annotation.
     */
    @Transactional
    public AnnotationEntity createAnnotation(AnnotationEntity annotation) {
        ProjectEntity project = projectMapper.selectById(annotation.getProjectId());
        if (project == null || project.getDeleted()) {
            throw BusinessException.notFound("项目不存在");
        }

        annotation.setId(IdWorker.getId());
        annotation.setStatus("open");
        annotation.setCreatedAt(LocalDateTime.now());
        annotation.setUpdatedAt(LocalDateTime.now());
        annotation.setDeleted(false);
        annotationMapper.insert(annotation);

        // Update project status to change_requested
        if (!"change_requested".equals(project.getStatus()) && !"confirmed".equals(project.getStatus())) {
            project.setStatus("change_requested");
            project.setUpdatedAt(LocalDateTime.now());
            projectMapper.updateById(project);
        }

        auditLogService.logCustomerAction(
                annotation.getStoreId(),
                "SUBMIT_ANNOTATION",
                "project",
                annotation.getProjectId(),
                annotation.getCustomerName(),
                "提交了新的修改意见: " + annotation.getContent()
        );

        return annotation;
    }

    /**
     * Designer resolves an annotation.
     */
    @Transactional
    public void resolveAnnotation(CurrentUser user, Long annotationId, String status) {
        AnnotationEntity entity = annotationMapper.selectOne(new LambdaQueryWrapper<AnnotationEntity>()
                .eq(AnnotationEntity::getId, annotationId)
                .eq(AnnotationEntity::getStoreId, user.storeId())
                .eq(AnnotationEntity::getDeleted, false));
        if (entity == null) {
            throw BusinessException.notFound("标注不存在");
        }

        entity.setStatus(status);
        entity.setResolvedBy(user.userId());
        entity.setResolvedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(user.userId());
        annotationMapper.updateById(entity);

        auditLogService.logUserAction(
                user,
                "RESOLVE_ANNOTATION",
                "project",
                entity.getProjectId(),
                "处理了意见: " + entity.getContent() + " (状态: " + status + ")"
        );
    }

    /**
     * Reply to an annotation.
     */
    @Transactional
    public AnnotationCommentEntity createComment(AnnotationCommentEntity comment) {
        comment.setId(IdWorker.getId());
        comment.setCreatedAt(LocalDateTime.now());
        annotationCommentMapper.insert(comment);
        return comment;
    }

    /**
     * List annotations for a project version.
     */
    public List<AnnotationEntity> listAnnotations(Long storeId, Long projectId, Long versionId) {
        return annotationMapper.selectList(new LambdaQueryWrapper<AnnotationEntity>()
                .eq(AnnotationEntity::getStoreId, storeId)
                .eq(AnnotationEntity::getProjectId, projectId)
                .eq(AnnotationEntity::getVersionId, versionId)
                .eq(AnnotationEntity::getDeleted, false)
                .orderByDesc(AnnotationEntity::getCreatedAt));
    }

    /**
     * List comments for an annotation.
     */
    public List<AnnotationCommentEntity> listComments(Long storeId, Long annotationId) {
        return annotationCommentMapper.selectList(new LambdaQueryWrapper<AnnotationCommentEntity>()
                .eq(AnnotationCommentEntity::getStoreId, storeId)
                .eq(AnnotationCommentEntity::getAnnotationId, annotationId)
                .orderByAsc(AnnotationCommentEntity::getCreatedAt));
    }
}
