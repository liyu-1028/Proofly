package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.PublicAnnotationRequest;
import com.lyllink.proofly.dto.req.PublicConfirmationRequest;
import com.lyllink.proofly.dto.resp.AnnotationResponse;
import com.lyllink.proofly.dto.resp.ConfirmationRecordResponse;
import com.lyllink.proofly.dto.resp.ProjectResponse;
import com.lyllink.proofly.dto.resp.ProjectVersionResponse;
import com.lyllink.proofly.dto.resp.PublicProjectReviewResponse;
import com.lyllink.proofly.entity.AnnotationEntity;
import com.lyllink.proofly.entity.ConfirmationRecordEntity;
import com.lyllink.proofly.entity.FileObjectEntity;
import com.lyllink.proofly.entity.ReviewLinkEntity;
import com.lyllink.proofly.service.AnnotationService;
import com.lyllink.proofly.service.ConfirmationService;
import com.lyllink.proofly.service.FileService;
import com.lyllink.proofly.service.ProjectService;
import com.lyllink.proofly.service.ProjectVersionService;
import com.lyllink.proofly.service.ReviewLinkService;
import com.lyllink.proofly.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/public/reviews/{token}")
public class PublicReviewController {

    private final ReviewLinkService reviewLinkService;
    private final ProjectService projectService;
    private final ProjectVersionService projectVersionService;
    private final AnnotationService annotationService;
    private final ConfirmationService confirmationService;
    private final FileService fileService;

    public PublicReviewController(
            ReviewLinkService reviewLinkService,
            ProjectService projectService,
            ProjectVersionService projectVersionService,
            AnnotationService annotationService,
            ConfirmationService confirmationService,
            FileService fileService
    ) {
        this.reviewLinkService = reviewLinkService;
        this.projectService = projectService;
        this.projectVersionService = projectVersionService;
        this.annotationService = annotationService;
        this.confirmationService = confirmationService;
        this.fileService = fileService;
    }

    @PostMapping("/files/upload")
    public ApiResponse<Map<String, String>> uploadFile(
            @PathVariable String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileRole", defaultValue = "attachment") String fileRole
    ) throws Exception {
        ReviewLinkEntity link = reviewLinkService.validateToken(token);
        
        FileObjectEntity entity = fileService.uploadPublicFile(
                link.getStoreId(),
                link.getProjectId(),
                link.getCurrentVersionId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream(),
                fileRole
        );
        
        String url = fileService.getFilePreviewUrl(entity.getObjectKey());
        
        Map<String, String> resp = new java.util.HashMap<>();
        resp.put("id", entity.getId().toString());
        resp.put("url", url);
        resp.put("objectKey", entity.getObjectKey());
        return ApiResponse.success(resp);
    }

    @GetMapping
    public ApiResponse<PublicProjectReviewResponse> getReviewData(@PathVariable String token) {
        ReviewLinkEntity link = reviewLinkService.validateToken(token);
        reviewLinkService.incrementAccessCount(link.getId());

        PublicProjectReviewResponse resp = new PublicProjectReviewResponse();
        // 使用不检查 CurrentUser 的内部详情方法
        resp.setProject(projectService.detailInternal(link.getStoreId(), link.getProjectId()));
        resp.setVersions(projectVersionService.listVersionsInternal(link.getStoreId(), link.getProjectId()));
        resp.setActiveVersionId(link.getCurrentVersionId());
        
        // 获取特定版本（或项目？）的标注
        // 通常，审稿链接指向“当前”版本。
        List<AnnotationResponse> annotations = annotationService.listAnnotations(link.getStoreId(), link.getProjectId(), link.getCurrentVersionId())
                .stream().map(this::toAnnotationResponse).collect(Collectors.toList());
        resp.setAnnotations(annotations);

        ConfirmationRecordEntity conf = confirmationService.getConfirmation(link.getStoreId(), link.getProjectId());
        if (conf != null) {
            resp.setConfirmation(toConfirmationResponse(conf));
        }

        return ApiResponse.success(resp);
    }

    @PostMapping("/annotations")
    public ApiResponse<AnnotationResponse> submitAnnotation(
            @PathVariable String token,
            @RequestBody PublicAnnotationRequest request
    ) {
        ReviewLinkEntity link = reviewLinkService.validateToken(token);

        AnnotationEntity entity = new AnnotationEntity();
        entity.setStoreId(link.getStoreId());
        entity.setProjectId(link.getProjectId());
        entity.setVersionId(link.getCurrentVersionId());
        entity.setReviewLinkId(link.getId());
        entity.setType(request.getType());
        entity.setXRatio(request.getXRatio());
        entity.setYRatio(request.getYRatio());
        entity.setWidthRatio(request.getWidthRatio());
        entity.setHeightRatio(request.getHeightRatio());
        entity.setContent(request.getContent());
        // 存储 ObjectKey
        entity.setMediaUrl(request.getMediaUrl());
        entity.setMediaDuration(request.getMediaDuration());
        entity.setCustomerName(request.getCustomerName());
        entity.setCustomerContact(request.getCustomerContact());

        return ApiResponse.success(toAnnotationResponse(annotationService.createAnnotation(entity)));
    }

    @PostMapping("/confirmations")
    public ApiResponse<ConfirmationRecordResponse> confirm(
            @PathVariable String token,
            @RequestBody PublicConfirmationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        ReviewLinkEntity link = reviewLinkService.validateToken(token);

        ConfirmationRecordEntity entity = new ConfirmationRecordEntity();
        entity.setStoreId(link.getStoreId());
        entity.setProjectId(link.getProjectId());
        entity.setVersionId(link.getCurrentVersionId());
        entity.setReviewLinkId(link.getId());
        entity.setCustomerName(request.getCustomerName());
        entity.setCustomerContact(request.getCustomerContact());
        entity.setIp(httpServletRequest.getRemoteAddr());
        entity.setUserAgent(httpServletRequest.getHeader("User-Agent"));

        return ApiResponse.success(toConfirmationResponse(confirmationService.confirm(entity)));
    }

    private AnnotationResponse toAnnotationResponse(AnnotationEntity entity) {
        AnnotationResponse resp = new AnnotationResponse();
        resp.setId(entity.getId());
        resp.setType(entity.getType());
        resp.setXRatio(entity.getXRatio());
        resp.setYRatio(entity.getYRatio());
        resp.setWidthRatio(entity.getWidthRatio());
        resp.setHeightRatio(entity.getHeightRatio());
        resp.setContent(entity.getContent());
        
        // 动态生成预签名 URL
        resp.setMediaUrl(fileService.getFilePreviewUrl(entity.getMediaUrl()));
        
        resp.setMediaDuration(entity.getMediaDuration());
        resp.setCustomerName(entity.getCustomerName());
        resp.setStatus(entity.getStatus());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setResolvedAt(entity.getResolvedAt());
        // resolvedByNickname 逻辑需要 userMapper，现在先保持简单，稍后再获取
        return resp;
    }

    private ConfirmationRecordResponse toConfirmationResponse(ConfirmationRecordEntity entity) {
        ConfirmationRecordResponse resp = new ConfirmationRecordResponse();
        resp.setId(entity.getId());
        resp.setVersionId(entity.getVersionId());
        resp.setCustomerName(entity.getCustomerName());
        resp.setConfirmedAt(entity.getConfirmedAt());
        return resp;
    }
}
