package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.ReviewLinkCreateRequest;
import com.lyllink.proofly.dto.resp.ReviewLinkResponse;
import com.lyllink.proofly.service.ReviewLinkService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class ReviewLinkController {

    private final ReviewLinkService reviewLinkService;

    public ReviewLinkController(ReviewLinkService reviewLinkService) {
        this.reviewLinkService = reviewLinkService;
    }

    @GetMapping("/projects/{projectId}/review-links")
    public ApiResponse<List<ReviewLinkResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.success(reviewLinkService.listLinks(CurrentUserHolder.required(), projectId));
    }

    @PostMapping("/projects/{projectId}/review-links")
    public ApiResponse<ReviewLinkResponse> generate(
            @PathVariable Long projectId,
            @RequestBody ReviewLinkCreateRequest request
    ) {
        return ApiResponse.success(reviewLinkService.generateLink(CurrentUserHolder.required(), projectId, request));
    }

    @PatchMapping("/review-links/{linkId}/disable")
    public ApiResponse<Void> disable(@PathVariable Long linkId) {
        reviewLinkService.disableLink(CurrentUserHolder.required(), linkId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/review-links/{linkId}/enable")
    public ApiResponse<Void> enable(@PathVariable Long linkId) {
        reviewLinkService.enableLink(CurrentUserHolder.required(), linkId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/review-links/{linkId}")
    public ApiResponse<Void> delete(@PathVariable Long linkId) {
        reviewLinkService.deleteLink(CurrentUserHolder.required(), linkId);
        return ApiResponse.success(null);
    }
}
