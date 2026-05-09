package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.config.ProoflyProperties;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.ReviewLinkMapper;
import com.lyllink.proofly.dto.req.ReviewLinkCreateRequest;
import com.lyllink.proofly.dto.resp.ReviewLinkResponse;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.entity.ReviewLinkEntity;
import com.lyllink.proofly.security.CurrentUser;
import com.lyllink.proofly.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewLinkService {

    private final ReviewLinkMapper reviewLinkMapper;
    private final ProjectMapper projectMapper;
    private final ProoflyProperties prooflyProperties;

    public ReviewLinkService(
            ReviewLinkMapper reviewLinkMapper,
            ProjectMapper projectMapper,
            ProoflyProperties prooflyProperties
    ) {
        this.reviewLinkMapper = reviewLinkMapper;
        this.projectMapper = projectMapper;
        this.prooflyProperties = prooflyProperties;
    }

    /**
     * List all review links for a project.
     */
    public List<ReviewLinkResponse> listLinks(CurrentUser currentUser, Long projectId) {
        ensureProjectExists(currentUser.storeId(), projectId);

        return reviewLinkMapper.selectList(new LambdaQueryWrapper<ReviewLinkEntity>()
                .eq(ReviewLinkEntity::getStoreId, currentUser.storeId())
                .eq(ReviewLinkEntity::getProjectId, projectId)
                .eq(ReviewLinkEntity::getDeleted, false)
                .orderByDesc(ReviewLinkEntity::getCreatedAt))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Generate a new review link for a project.
     */
    @Transactional
    public ReviewLinkResponse generateLink(CurrentUser currentUser, Long projectId, ReviewLinkCreateRequest request) {
        ProjectEntity project = requiredProject(currentUser.storeId(), projectId);

        String token = SecurityUtils.generateToken();
        String tokenHash = SecurityUtils.sha256(token);

        ReviewLinkEntity entity = new ReviewLinkEntity();
        entity.setId(IdWorker.getId());
        entity.setStoreId(currentUser.storeId());
        entity.setProjectId(projectId);
        entity.setCurrentVersionId(project.getCurrentVersionId());
        entity.setTokenHash(tokenHash);
        entity.setStatus("active");
        entity.setExpiresAt(request.getExpiresAt());
        entity.setMaxAccessCount(request.getMaxAccessCount());
        entity.setAccessCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(currentUser.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser.userId());
        entity.setDeleted(false);

        reviewLinkMapper.insert(entity);

        ReviewLinkResponse response = toResponse(entity);
        response.setToken(token); // Return plain token only once
        response.setUrl(prooflyProperties.getReviewBaseUrl() + token);
        return response;
    }

    /**
     * Disable a review link.
     */
    @Transactional
    public void disableLink(CurrentUser currentUser, Long linkId) {
        ReviewLinkEntity entity = requiredLink(currentUser.storeId(), linkId);
        entity.setStatus("disabled");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser.userId());
        reviewLinkMapper.updateById(entity);
    }

    /**
     * Enable a review link.
     */
    @Transactional
    public void enableLink(CurrentUser currentUser, Long linkId) {
        ReviewLinkEntity entity = requiredLink(currentUser.storeId(), linkId);
        entity.setStatus("active");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser.userId());
        reviewLinkMapper.updateById(entity);
    }

    /**
     * Delete a review link (logical delete).
     */
    @Transactional
    public void deleteLink(CurrentUser currentUser, Long linkId) {
        ReviewLinkEntity entity = requiredLink(currentUser.storeId(), linkId);
        entity.setDeleted(true);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser.userId());
        reviewLinkMapper.updateById(entity);
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

    private void ensureProjectExists(Long storeId, Long projectId) {
        requiredProject(storeId, projectId);
    }

    private ReviewLinkEntity requiredLink(Long storeId, Long linkId) {
        ReviewLinkEntity entity = reviewLinkMapper.selectOne(new LambdaQueryWrapper<ReviewLinkEntity>()
                .eq(ReviewLinkEntity::getStoreId, storeId)
                .eq(ReviewLinkEntity::getId, linkId)
                .eq(ReviewLinkEntity::getDeleted, false)
                .last("LIMIT 1"));
        if (entity == null) {
            throw BusinessException.notFound("审稿链接不存在");
        }
        return entity;
    }

    private ReviewLinkResponse toResponse(ReviewLinkEntity entity) {
        ReviewLinkResponse resp = new ReviewLinkResponse();
        resp.setId(entity.getId());
        resp.setProjectId(entity.getProjectId());
        resp.setCurrentVersionId(entity.getCurrentVersionId());
        resp.setStatus(entity.getStatus());
        resp.setExpiresAt(entity.getExpiresAt());
        resp.setMaxAccessCount(entity.getMaxAccessCount());
        resp.setAccessCount(entity.getAccessCount());
        resp.setLastAccessAt(entity.getLastAccessAt());
        resp.setCreatedAt(entity.getCreatedAt());
        // URL can't be rebuilt perfectly without the token, which we don't store plain.
        // For existing links, we might just return a "token prefix" or similar if needed for UI identification,
        // but typically you just show the list and status.
        // If the user lost the link, they should generate a new one.
        return resp;
    }
}
