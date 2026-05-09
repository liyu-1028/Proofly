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
import com.lyllink.proofly.entity.ReviewLinkEntity;
import com.lyllink.proofly.service.AnnotationService;
import com.lyllink.proofly.service.ConfirmationService;
import com.lyllink.proofly.service.ProjectService;
import com.lyllink.proofly.service.ProjectVersionService;
import com.lyllink.proofly.service.ReviewLinkService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/reviews/{token}")
public class PublicReviewController {

    private final ReviewLinkService reviewLinkService;
    private final ProjectService projectService;
    private final ProjectVersionService projectVersionService;
    private final AnnotationService annotationService;
    private final ConfirmationService confirmationService;

    public PublicReviewController(
            ReviewLinkService reviewLinkService,
            ProjectService projectService,
            ProjectVersionService projectVersionService,
            AnnotationService annotationService,
            ConfirmationService confirmationService
    ) {
        this.reviewLinkService = reviewLinkService;
        this.projectService = projectService;
        this.projectVersionService = projectVersionService;
        this.annotationService = annotationService;
        this.confirmationService = confirmationService;
    }

    @GetMapping
    public ApiResponse<PublicProjectReviewResponse> getReviewData(@PathVariable String token) {
        ReviewLinkEntity link = reviewLinkService.validateToken(token);
        reviewLinkService.incrementAccessCount(link.getId());

        PublicProjectReviewResponse resp = new PublicProjectReviewResponse();
        // Use an internal detail method that doesn't check CurrentUser
        resp.setProject(projectService.detailInternal(link.getStoreId(), link.getProjectId()));
        resp.setVersions(projectVersionService.listVersionsInternal(link.getStoreId(), link.getProjectId()));
        resp.setActiveVersionId(link.getCurrentVersionId());
        
        // Get annotations for the specific version (or project?)
        // Usually, a review link points to a "current" version.
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
        resp.setCustomerName(entity.getCustomerName());
        resp.setStatus(entity.getStatus());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setResolvedAt(entity.getResolvedAt());
        // resolvedByNickname logic would need userMapper, let's keep it simple for now or fetch later
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
