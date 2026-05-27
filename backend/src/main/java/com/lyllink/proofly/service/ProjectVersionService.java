package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.AnnotationMapper;
import com.lyllink.proofly.dao.FileObjectMapper;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.ProjectVersionMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.dto.resp.ProjectVersionResponse;
import com.lyllink.proofly.entity.AnnotationEntity;
import com.lyllink.proofly.entity.FileObjectEntity;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.entity.ProjectVersionEntity;
import com.lyllink.proofly.entity.UserEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectVersionService {

    private final ProjectVersionMapper projectVersionMapper;
    private final ProjectMapper projectMapper;
    private final FileService fileService;
    private final FileObjectMapper fileObjectMapper;
    private final UserMapper userMapper;
    private final AnnotationMapper annotationMapper;

    public ProjectVersionService(
            ProjectVersionMapper projectVersionMapper,
            ProjectMapper projectMapper,
            FileService fileService,
            FileObjectMapper fileObjectMapper,
            UserMapper userMapper,
            AnnotationMapper annotationMapper
    ) {
        this.projectVersionMapper = projectVersionMapper;
        this.projectMapper = projectMapper;
        this.fileService = fileService;
        this.fileObjectMapper = fileObjectMapper;
        this.userMapper = userMapper;
        this.annotationMapper = annotationMapper;
    }

    /**
     * 列出项目的所有版本。
     */
    public List<ProjectVersionResponse> listVersions(CurrentUser currentUser, Long projectId) {
        return listVersionsInternal(currentUser.storeId(), projectId);
    }

    public List<ProjectVersionResponse> listVersionsInternal(Long storeId, Long projectId) {
        ensureProjectExists(storeId, projectId);

        List<ProjectVersionEntity> versions = projectVersionMapper.selectList(new LambdaQueryWrapper<ProjectVersionEntity>()
                .eq(ProjectVersionEntity::getStoreId, storeId)
                .eq(ProjectVersionEntity::getProjectId, projectId)
                .eq(ProjectVersionEntity::getDeleted, false)
                .orderByDesc(ProjectVersionEntity::getVersionNo));

        if (versions.isEmpty()) {
            return List.of();
        }

        // 批量获取文件和用户昵称
        Set<Long> fileIds = versions.stream().map(ProjectVersionEntity::getFileId).collect(Collectors.toSet());
        Map<Long, FileObjectEntity> files = fileObjectMapper.selectBatchIds(fileIds).stream()
                .collect(Collectors.toMap(FileObjectEntity::getId, f -> f));

        Set<Long> userIds = versions.stream().map(ProjectVersionEntity::getUploadedBy).collect(Collectors.toSet());
        Map<Long, String> nicknames = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getId, userIds)
                .select(UserEntity::getId, UserEntity::getNickname))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));

        return versions.stream()
                .map(v -> {
                    ProjectVersionResponse resp = toResponse(v, files.get(v.getFileId()), nicknames);
                    // 填充统计信息
                    resp.setAnnotationCount(Math.toIntExact(annotationMapper.selectCount(new LambdaQueryWrapper<AnnotationEntity>()
                            .eq(AnnotationEntity::getVersionId, v.getId())
                            .eq(AnnotationEntity::getDeleted, false))));
                    resp.setHasVoice(annotationMapper.selectCount(new LambdaQueryWrapper<AnnotationEntity>()
                            .eq(AnnotationEntity::getVersionId, v.getId())
                            .isNotNull(AnnotationEntity::getMediaUrl)
                            .ne(AnnotationEntity::getMediaUrl, "")
                            .eq(AnnotationEntity::getDeleted, false)) > 0);
                    return resp;
                })
                .toList();
    }

    /**
     * 为项目上传新版本。
     */
    @Transactional
    public ProjectVersionResponse uploadVersion(
            CurrentUser currentUser,
            Long projectId,
            String description,
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream
    ) {
        ProjectEntity project = requiredProject(currentUser.storeId(), projectId);
        if ("archived".equals(project.getStatus())) {
            throw BusinessException.badRequest("已归档的项目不能上传新版本");
        }
        if (project.getConfirmedVersionId() != null) {
            throw BusinessException.badRequest("已确认定稿的项目不能上传新版本");
        }

        // 1. 准备版本记录
        Long versionId = IdWorker.getId();
        Integer nextVersionNo = getNextVersionNo(currentUser.storeId(), projectId);
        String versionName = "V" + nextVersionNo;

        // 2. 上传文件（通过 versionId 建立事务关联）
        FileObjectEntity fileObject = fileService.uploadFile(
                currentUser, projectId, versionId, originalFilename, contentType, size, inputStream, "original");

        // 3. 创建版本实体
        ProjectVersionEntity version = new ProjectVersionEntity();
        version.setId(versionId);
        version.setStoreId(currentUser.storeId());
        version.setProjectId(projectId);
        version.setVersionNo(nextVersionNo);
        version.setVersionName(versionName);
        version.setFileId(fileObject.getId());
        version.setUploadedBy(currentUser.userId());
        version.setDescription(description);
        version.setIsCurrent(true);
        version.setIsConfirmed(false);
        version.setCreatedAt(LocalDateTime.now());
        version.setCreatedBy(currentUser.userId());
        version.setUpdatedAt(LocalDateTime.now());
        version.setUpdatedBy(currentUser.userId());
        version.setDeleted(false);

        // 4. 更新其他版本为非当前版本
        projectVersionMapper.update(null, new LambdaUpdateWrapper<ProjectVersionEntity>()
                .eq(ProjectVersionEntity::getProjectId, projectId)
                .set(ProjectVersionEntity::getIsCurrent, false));

        projectVersionMapper.insert(version);

        // 5. 更新项目状态和 current_version_id
        project.setCurrentVersionId(versionId);
        if ("draft".equals(project.getStatus())) {
            project.setStatus("waiting_feedback");
        }
        project.setUpdatedBy(currentUser.userId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        ProjectVersionResponse resp = toResponse(version, fileObject, Map.of(currentUser.userId(), currentUser.nickname()));
        resp.setAnnotationCount(0);
        resp.setHasVoice(false);
        return resp;
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

    private Integer getNextVersionNo(Long storeId, Long projectId) {
        Integer maxNo = projectVersionMapper.selectList(new LambdaQueryWrapper<ProjectVersionEntity>()
                .eq(ProjectVersionEntity::getProjectId, projectId)
                .select(ProjectVersionEntity::getVersionNo)
                .orderByDesc(ProjectVersionEntity::getVersionNo)
                .last("LIMIT 1"))
                .stream().map(ProjectVersionEntity::getVersionNo).findFirst().orElse(0);
        return maxNo + 1;
    }

    private ProjectVersionResponse toResponse(ProjectVersionEntity v, FileObjectEntity f, Map<Long, String> nicknames) {
        ProjectVersionResponse resp = new ProjectVersionResponse();
        resp.setId(v.getId());
        resp.setStoreId(v.getStoreId());
        resp.setProjectId(v.getProjectId());
        resp.setVersionNo(v.getVersionNo());
        resp.setVersionName(v.getVersionName());
        resp.setFileId(v.getFileId());
        if (f != null) {
            resp.setOriginalFilename(f.getOriginalFilename());
            resp.setFileExt(f.getFileExt());
            resp.setFileSize(f.getFileSize());
            resp.setPreviewUrl(fileService.getFilePreviewUrl(f.getObjectKey()));
        }
        resp.setUploadedBy(v.getUploadedBy());
        resp.setUploaderNickname(nicknames.getOrDefault(v.getUploadedBy(), "未知"));
        resp.setDescription(v.getDescription());
        resp.setIsCurrent(v.getIsCurrent());
        resp.setIsConfirmed(v.getIsConfirmed());
        resp.setConfirmedAt(v.getConfirmedAt());
        resp.setCreatedAt(v.getCreatedAt());
        return resp;
    }
}
