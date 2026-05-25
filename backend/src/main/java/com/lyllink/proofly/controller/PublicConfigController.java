package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.resp.SystemConfigResponse;
import com.lyllink.proofly.service.SystemConfigService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/configs")
public class PublicConfigController {

    private final SystemConfigService systemConfigService;

    public PublicConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/brand/{storeId}")
    public ApiResponse<Map<String, String>> getBrandConfig(@PathVariable Long storeId) {
        List<SystemConfigResponse> configs = systemConfigService.listMergedConfigs(storeId);
        
        Map<String, String> brandConfig = configs.stream()
                .filter(c -> c.getConfigKey().startsWith("brand."))
                .collect(Collectors.toMap(
                        SystemConfigResponse::getConfigKey,
                        SystemConfigResponse::getConfigValue,
                        (v1, v2) -> v1
                ));
        
        return ApiResponse.success(brandConfig);
    }
}
