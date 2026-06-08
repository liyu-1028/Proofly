package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.PaymentOrderMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.dto.req.OrderCreateRequest;
import com.lyllink.proofly.dto.resp.OrderResponse;
import com.lyllink.proofly.dto.resp.OrderStatusResponse;
import com.lyllink.proofly.entity.PaymentOrderEntity;
import com.lyllink.proofly.entity.StoreEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lyllink.proofly.config.ProoflyProperties;

@Service
public class BillingService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final StoreMapper storeMapper;
    private final NotificationService notificationService;
    private final ProoflyProperties prooflyProperties;

    public BillingService(
            PaymentOrderMapper paymentOrderMapper,
            StoreMapper storeMapper,
            NotificationService notificationService,
            ProoflyProperties prooflyProperties
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.storeMapper = storeMapper;
        this.notificationService = notificationService;
        this.prooflyProperties = prooflyProperties;
    }

    /**
     * 创建订单
     */
    @Transactional
    public OrderResponse createOrder(CurrentUser currentUser, OrderCreateRequest request) {
        StoreEntity store = storeMapper.selectById(currentUser.storeId());
        if (store == null || Boolean.TRUE.equals(store.getDeleted())) {
            throw BusinessException.notFound("门店不存在");
        }

        int months = request.getDurationMonths();
        if (months <= 0) {
            throw BusinessException.badRequest("购买月数必须大于 0");
        }

        BigDecimal amount = calculateAmount(months);
        String orderNo = "ORDER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(IdWorker.getId());
        order.setStoreId(currentUser.storeId());
        order.setOrderNo(orderNo);
        order.setPlanType("pro");
        order.setAmount(amount);
        order.setDurationMonths(months);
        order.setStatus("pending");
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "wechat");
        order.setCreatedAt(LocalDateTime.now());
        order.setCreatedBy(currentUser.userId());
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentUser.userId());
        order.setDeleted(false);

        paymentOrderMapper.insert(order);

        // 获取真实的 apiUrl 
        String apiUrl = prooflyProperties.getBilling().getXpay().getApiUrl();
        String payUrl = "";

        if (apiUrl == null || apiUrl.contains("mock") || apiUrl.isEmpty()) {
            payUrl = "/api/public/webhook/xpay/mock-pay?orderNo=" + orderNo;
        } else {
            // 真实调用 XPay
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

                String appId = prooflyProperties.getBilling().getXpay().getAppId();
                String appKey = prooflyProperties.getBilling().getXpay().getAppKey();
                // 使用在方案中规划好的二级域名
                String notifyUrl = "https://proofly.lyllink.top/api/public/webhook/xpay";

                // 签名：MD5(orderNo + amount.toString() + paymentMethod + appKey)
                String paymentMethod = order.getPaymentMethod();
                String rawStr = orderNo + amount.toString() + paymentMethod + appKey;
                String sign = org.springframework.util.DigestUtils.md5DigestAsHex(rawStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("appId", appId);
                params.put("orderNo", orderNo);
                params.put("amount", amount);
                params.put("paymentMethod", paymentMethod);
                params.put("notifyUrl", notifyUrl);
                params.put("sign", sign);

                // 请求三方
                org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.postForEntity(apiUrl, params, java.util.Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.util.Map body = response.getBody();
                    if (body.get("code") != null && (int) body.get("code") == 0) {
                        java.util.Map data = (java.util.Map) body.get("data");
                        if (data != null && data.get("payUrl") != null) {
                            payUrl = data.get("payUrl").toString();
                        }
                    }
                }
            } catch (Exception e) {
                payUrl = "";
            }

            if (payUrl == null || payUrl.isEmpty()) {
                payUrl = "/api/public/webhook/xpay/mock-pay?orderNo=" + orderNo;
            }
        }

        return toResponse(order, payUrl);
    }

    /**
     * 处理支付回调（Webhook 核心逻辑）
     */
    @Transactional
    public void handleWebhook(String orderNo, String paymentMethod, String outTradeNo) {
        PaymentOrderEntity order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getOrderNo, orderNo)
                .eq(PaymentOrderEntity::getDeleted, false));

        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        // 幂等控制：如果已经是 paid，直接成功返回
        if ("paid".equals(order.getStatus())) {
            return;
        }

        order.setStatus("paid");
        order.setPaymentMethod(paymentMethod);
        order.setOutTradeNo(outTradeNo);
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        paymentOrderMapper.updateById(order);

        // 更新门店的套餐到期时间
        StoreEntity store = storeMapper.selectById(order.getStoreId());
        if (store != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentExpiresAt = store.getPlanExpiresAt();

            // 若之前已过期或无过期时间，则从当前时间开始加
            if (currentExpiresAt == null || currentExpiresAt.isBefore(now)) {
                store.setPlanExpiresAt(now.plusMonths(order.getDurationMonths()));
            } else {
                store.setPlanExpiresAt(currentExpiresAt.plusMonths(order.getDurationMonths()));
            }

            store.setPlanType("pro");
            store.setUpdatedAt(now);
            storeMapper.updateById(store);

            // 发送系统通知给 Owner
            Long receiverId = store.getCreatedBy();
            if (receiverId != null) {
                notificationService.create(
                        store.getId(),
                        receiverId,
                        null,
                        "BILLING_PAID",
                        "套餐购买/续费成功",
                        String.format("您的门店已成功购买/续费 %d 个月 Pro 高级套餐。有效期延至：%s。",
                                order.getDurationMonths(), store.getPlanExpiresAt().toLocalDate().toString())
                );
            }
        }
    }

    /**
     * 查询订单支付状态
     */
    public OrderStatusResponse queryOrderStatus(Long storeId, String orderNo) {
        PaymentOrderEntity order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getStoreId, storeId)
                .eq(PaymentOrderEntity::getOrderNo, orderNo)
                .eq(PaymentOrderEntity::getDeleted, false));

        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }

        boolean isPaid = "paid".equals(order.getStatus());
        return new OrderStatusResponse(orderNo, order.getStatus(), isPaid);
    }

    /**
     * 查询账单历史
     */
    public List<OrderResponse> getOrders(Long storeId) {
        List<PaymentOrderEntity> list = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getStoreId, storeId)
                .eq(PaymentOrderEntity::getDeleted, false)
                .orderByDesc(PaymentOrderEntity::getCreatedAt));

        return list.stream()
                .map(order -> toResponse(order, null))
                .toList();
    }

    private BigDecimal calculateAmount(int months) {
        BigDecimal basePrice = prooflyProperties.getBilling().getBasePricePerMonth();
        if (basePrice == null) {
            basePrice = new BigDecimal("29.00");
        }

        BigDecimal discount = BigDecimal.ONE;
        if (prooflyProperties.getBilling().getDiscounts() != null) {
            BigDecimal d = prooflyProperties.getBilling().getDiscounts().get(months);
            if (d != null) {
                discount = d;
            }
        }

        return basePrice.multiply(new BigDecimal(months))
                .multiply(discount)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private OrderResponse toResponse(PaymentOrderEntity order, String payUrl) {
        return new OrderResponse(
                order.getId(),
                order.getStoreId(),
                order.getOrderNo(),
                order.getPlanType(),
                order.getAmount(),
                order.getDurationMonths(),
                order.getStatus(),
                order.getPaymentMethod(),
                payUrl,
                order.getPaidAt(),
                order.getCreatedAt()
        );
    }
}
