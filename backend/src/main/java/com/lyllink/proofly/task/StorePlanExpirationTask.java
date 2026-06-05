package com.lyllink.proofly.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.entity.StoreEntity;
import com.lyllink.proofly.service.NotificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorePlanExpirationTask {

    private final StoreMapper storeMapper;
    private final NotificationService notificationService;

    public StorePlanExpirationTask(StoreMapper storeMapper, NotificationService notificationService) {
        this.storeMapper = storeMapper;
        this.notificationService = notificationService;
    }

    /**
     * 每天凌晨 1 点执行一次检查
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkExpirations() {
        log.info("开始检查门店套餐过期情况...");
        List<StoreEntity> stores = storeMapper.selectList(new LambdaQueryWrapper<StoreEntity>()
                .eq(StoreEntity::getPlanType, "pro")
                .eq(StoreEntity::getDeleted, false));

        LocalDate today = LocalDate.now();
        for (StoreEntity store : stores) {
            LocalDateTime expiresAt = store.getPlanExpiresAt();
            if (expiresAt == null) {
                continue;
            }

            LocalDate expireDate = expiresAt.toLocalDate();
            if (expireDate.isBefore(today)) {
                // 已过期，将其恢复为 free
                store.setPlanType("free");
                store.setUpdatedAt(LocalDateTime.now());
                storeMapper.updateById(store);
                log.info("门店 [{}] 套餐已过期，降级为免费版", store.getName());

                // 发送过期通知
                Long ownerId = store.getCreatedBy();
                if (ownerId != null) {
                    notificationService.create(
                            store.getId(),
                            ownerId,
                            null,
                            "BILLING_EXPIRED",
                            "套餐已过期降级",
                            String.format("您的门店 Pro 高级套餐已于 %s 到期，现已降级为免费版。为避免项目数和员工数受到限制，请及时续费。", expireDate.toString())
                    );
                }
                continue;
            }

            long daysLeft = ChronoUnit.DAYS.between(today, expireDate);
            if (daysLeft == 7 || daysLeft == 3 || daysLeft == 1) {
                Long ownerId = store.getCreatedBy();
                if (ownerId != null) {
                    notificationService.create(
                            store.getId(),
                            ownerId,
                            null,
                            "BILLING_EXPIRING",
                            "套餐即将到期提醒",
                            String.format("您的门店 Pro 高级套餐还有 %d 天到期（到期日：%s），请及时续费以维持正常使用。", daysLeft, expireDate.toString())
                    );
                    log.info("门店 [{}] 套餐还有 {} 天到期，已发送提醒通知", store.getName(), daysLeft);
                }
            }
        }
        log.info("检查门店套餐过期情况完成。");
    }
}
