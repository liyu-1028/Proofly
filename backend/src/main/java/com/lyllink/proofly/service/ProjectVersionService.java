package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.FileObjectMapper;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.ProjectVersionMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.dto.resp.ProjectVersionResponse;
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

    public ProjectVersionService(
            ProjectVersionMapper projectVersionMapper,
            ProjectMapper projectMapper,
            FileService fileService,
            FileObjectMapper fileObjectMapper,
            UserMapper userMapper
    ) {
        this.projectVersionMapper = projectVersionMapper;
        this.projectMapper = projectMapper;
        this.fileService = fileService;
        this.fileObjectMapper = fileObjectMapper;
        this.userMapper = userMapper;
    }

    /**
     * List all versions for a project.
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

        // Batch fetch files and user nicknames
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
                .map(v -> toResponse(v, files.get(v.getFileId()), nicknames))
                .toList();
    }

    /**
     * Upload a new version for a project.
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

        // 1. Prepare version record
        Long versionId = IdWorker.getId();
        Integer nextVersionNo = getNextVersionNo(currentUser.storeId(), projectId);
        String versionName = "V" + nextVersionNo;

        // 2. Upload file (transactionally linked via versionId)
        FileObjectEntity fileObject = fileService.uploadFile(
                currentUser, projectId, versionId, originalFilename, contentType, size, inputStream, "original");

        // 3. Create version entity
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

        // 4. Update other versions to not current
        projectVersionMapper.update(null, new LambdaUpdateWrapper<ProjectVersionEntity>()
                .eq(ProjectVersionEntity::getProjectId, projectId)
                .set(ProjectVersionEntity::getIsCurrent, false));

        projectVersionMapper.insert(version);

        // 5. Update project status and current_version_id
        project.setCurrentVersionId(versionId);
        if ("draft".equals(project.getStatus())) {
            project.setStatus("waiting_feedback");
        }
        project.setUpdatedBy(currentUser.userId());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);

        return toResponse(version, fileObject, Map.of(currentUser.userId(), currentUser.nickname()));
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
