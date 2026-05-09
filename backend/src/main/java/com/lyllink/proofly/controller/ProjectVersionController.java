package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.resp.ProjectVersionResponse;
import com.lyllink.proofly.service.ProjectVersionService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/versions")
public class ProjectVersionController {

    private final ProjectVersionService projectVersionService;

    public ProjectVersionController(ProjectVersionService projectVersionService) {
        this.projectVersionService = projectVersionService;
    }

    @GetMapping
    public ApiResponse<List<ProjectVersionResponse>> list(@PathVariable Long projectId) {
        return ApiResponse.success(projectVersionService.listVersions(CurrentUserHolder.required(), projectId));
    }

    @PostMapping
    public ApiResponse<ProjectVersionResponse> upload(
            @PathVariable Long projectId,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ApiResponse.success(projectVersionService.uploadVersion(
                CurrentUserHolder.required(),
                projectId,
                description,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        ));
    }
}
