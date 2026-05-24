package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.SystemConfigUpdateRequest;
import com.lyllink.proofly.dto.resp.SystemConfigResponse;
import com.lyllink.proofly.security.CurrentUser;
import com.lyllink.proofly.service.SystemConfigService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ApiResponse<List<SystemConfigResponse>> list() {
        CurrentUser user = CurrentUserHolder.required();
        return ApiResponse.success(systemConfigService.listMergedConfigs(user.storeId()));
    }

    @PutMapping("/{key}")
    public ApiResponse<SystemConfigResponse> update(
            @PathVariable String key,
            @Valid @RequestBody SystemConfigUpdateRequest request
    ) {
        CurrentUser user = CurrentUserHolder.required();
        return ApiResponse.success(systemConfigService.updateStoreConfig(user.storeId(), key, request, user.userId()));
    }
}
