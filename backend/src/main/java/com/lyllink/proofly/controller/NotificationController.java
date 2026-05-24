package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.resp.NotificationResponse;
import com.lyllink.proofly.security.CurrentUser;
import com.lyllink.proofly.service.NotificationService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        CurrentUser user = CurrentUserHolder.required();
        return ApiResponse.success(notificationService.list(user.storeId(), user.userId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        CurrentUser user = CurrentUserHolder.required();
        return ApiResponse.success(notificationService.getUnreadCount(user.storeId(), user.userId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        CurrentUser user = CurrentUserHolder.required();
        notificationService.markAsRead(user.storeId(), user.userId(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/mark-all-read")
    public ApiResponse<Void> markAllAsRead() {
        CurrentUser user = CurrentUserHolder.required();
        notificationService.markAllAsRead(user.storeId(), user.userId());
        return ApiResponse.success(null);
    }
}
