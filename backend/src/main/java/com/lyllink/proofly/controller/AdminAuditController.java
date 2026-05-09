package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.entity.AuditLogEntity;
import com.lyllink.proofly.service.AuditLogService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/timeline")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogEntity>> getTimeline(@PathVariable Long projectId) {
        return ApiResponse.success(auditLogService.getProjectTimeline(
                CurrentUserHolder.required().storeId(), projectId));
    }
}
