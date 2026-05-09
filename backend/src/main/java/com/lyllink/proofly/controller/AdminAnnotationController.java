package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.resp.AnnotationResponse;
import com.lyllink.proofly.entity.AnnotationEntity;
import com.lyllink.proofly.service.AnnotationService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/versions/{versionId}/annotations")
public class AdminAnnotationController {

    private final AnnotationService annotationService;

    public AdminAnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @GetMapping
    public ApiResponse<List<AnnotationResponse>> list(
            @PathVariable Long projectId,
            @PathVariable Long versionId
    ) {
        List<AnnotationResponse> list = annotationService.listAnnotations(
                CurrentUserHolder.required().storeId(), projectId, versionId)
                .stream().map(this::toAnnotationResponse).collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @PatchMapping("/{annotationId}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long annotationId,
            @RequestParam String status
    ) {
        annotationService.resolveAnnotation(CurrentUserHolder.required(), annotationId, status);
        return ApiResponse.success(null);
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
        return resp;
    }
}
