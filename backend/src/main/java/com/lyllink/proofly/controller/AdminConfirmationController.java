package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.resp.ConfirmationRecordResponse;
import com.lyllink.proofly.entity.ConfirmationRecordEntity;
import com.lyllink.proofly.service.ConfirmationService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/confirmation")
public class AdminConfirmationController {

    private final ConfirmationService confirmationService;

    public AdminConfirmationController(ConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @GetMapping
    public ApiResponse<ConfirmationRecordResponse> get(@PathVariable Long projectId) {
        ConfirmationRecordEntity entity = confirmationService.getConfirmation(
                CurrentUserHolder.required().storeId(), projectId);
        if (entity == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(toResponse(entity));
    }

    private ConfirmationRecordResponse toResponse(ConfirmationRecordEntity entity) {
        ConfirmationRecordResponse resp = new ConfirmationRecordResponse();
        resp.setId(entity.getId());
        resp.setVersionId(entity.getVersionId());
        resp.setCustomerName(entity.getCustomerName());
        resp.setConfirmedAt(entity.getConfirmedAt());
        return resp;
    }
}
