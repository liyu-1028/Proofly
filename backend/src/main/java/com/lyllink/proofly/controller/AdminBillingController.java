package com.lyllink.proofly.controller;

import com.lyllink.proofly.common.ApiResponse;
import com.lyllink.proofly.dto.req.OrderCreateRequest;
import com.lyllink.proofly.dto.resp.OrderResponse;
import com.lyllink.proofly.dto.resp.OrderStatusResponse;
import com.lyllink.proofly.security.CurrentUser;
import com.lyllink.proofly.service.BillingService;
import com.lyllink.proofly.utils.CurrentUserHolder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing")
public class AdminBillingController {

    private final BillingService billingService;

    public AdminBillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * 创建套餐支付订单
     */
    @PostMapping("/orders")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        CurrentUser user = CurrentUserHolder.required();
        // 校验权限：只有 owner 或者是 admin 可以购买或续费
        if (!user.roles().contains("owner") && !user.roles().contains("admin")) {
            throw com.lyllink.proofly.common.BusinessException.forbidden("只有门店老板可以购买/续费套餐");
        }
        return ApiResponse.success(billingService.createOrder(user, request));
    }

    /**
     * 查询订单支付状态
     */
    @GetMapping("/orders/{orderNo}/status")
    public ApiResponse<OrderStatusResponse> queryOrderStatus(@PathVariable("orderNo") String orderNo) {
        CurrentUser user = CurrentUserHolder.required();
        return ApiResponse.success(billingService.queryOrderStatus(user.storeId(), orderNo));
    }

    /**
     * 获取门店的历史账单
     */
    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> getOrders() {
        CurrentUser user = CurrentUserHolder.required();
        return ApiResponse.success(billingService.getOrders(user.storeId()));
    }
}
