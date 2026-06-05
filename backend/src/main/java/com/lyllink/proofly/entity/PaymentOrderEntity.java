package com.lyllink.proofly.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("payment_order")
public class PaymentOrderEntity {

    @TableId
    private Long id;
    private Long storeId;
    private String orderNo;
    private String planType;
    private BigDecimal amount;
    private Integer durationMonths;
    private String status;
    private String paymentMethod;
    private String outTradeNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Boolean deleted;
}
