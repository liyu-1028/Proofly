package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.UserCreateRequest;
import com.lyllink.proofly.dto.req.UserResetPasswordRequest;
import com.lyllink.proofly.dto.req.UserStatusUpdateRequest;
import com.lyllink.proofly.dto.req.UserUpdateRequest;
import com.lyllink.proofly.dto.resp.UserResponse;
import com.lyllink.proofly.service.UserService;
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
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(userService.list(CurrentUserHolder.required(), keyword, status));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(CurrentUserHolder.required(), request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> detail(@PathVariable Long userId) {
        return ApiResponse.success(userService.detail(CurrentUserHolder.required(), userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> update(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(CurrentUserHolder.required(), userId, request));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateStatus(CurrentUserHolder.required(), userId, request));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserResetPasswordRequest request
    ) {
        userService.resetPassword(CurrentUserHolder.required(), userId, request);
        return ApiResponse.success(null);
    }
}
