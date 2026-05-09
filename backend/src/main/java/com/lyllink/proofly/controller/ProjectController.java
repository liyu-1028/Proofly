package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.ProjectCreateRequest;
import com.lyllink.proofly.dto.req.ProjectUpdateRequest;
import com.lyllink.proofly.dto.resp.ProjectResponse;
import com.lyllink.proofly.service.ProjectService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerUserId
    ) {
        return ApiResponse.success(projectService.list(CurrentUserHolder.required(), keyword, status, ownerUserId));
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.create(CurrentUserHolder.required(), request));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> detail(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.detail(CurrentUserHolder.required(), projectId));
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        return ApiResponse.success(projectService.update(CurrentUserHolder.required(), projectId, request));
    }

    @PatchMapping("/{projectId}/archive")
    public ApiResponse<ProjectResponse> archive(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.archive(CurrentUserHolder.required(), projectId));
    }

    @PatchMapping("/{projectId}/restore")
    public ApiResponse<ProjectResponse> restore(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.restore(CurrentUserHolder.required(), projectId));
    }
}
