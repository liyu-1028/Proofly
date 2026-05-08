package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.StoreUpdateRequest;
import com.lyllink.proofly.dto.resp.StoreResponse;
import com.lyllink.proofly.service.StoreService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/current")
    public ApiResponse<StoreResponse> current() {
        return ApiResponse.success(storeService.current(CurrentUserHolder.required()));
    }

    @PutMapping("/current")
    public ApiResponse<StoreResponse> updateCurrent(@Valid @RequestBody StoreUpdateRequest request) {
        return ApiResponse.success(storeService.updateCurrent(CurrentUserHolder.required(), request));
    }
}
