package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.dao.ReferralRecordMapper;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.entity.ReferralRecordEntity;
import com.lyllink.proofly.entity.StoreEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralService {

    private final ReferralRecordMapper referralRecordMapper;
    private final StoreMapper storeMapper;
    private final NotificationService notificationService;

    public ReferralService(
            ReferralRecordMapper referralRecordMapper,
            StoreMapper storeMapper,
            NotificationService notificationService
    ) {
        this.referralRecordMapper = referralRecordMapper;
        this.storeMapper = storeMapper;
        this.notificationService = notificationService;
    }

    /**
     * Bind referral relationship upon registration.
     */
    public void bindReferral(Long inviteeStoreId, String inviteCode) {
        StoreEntity inviterStore = storeMapper.selectOne(new LambdaQueryWrapper<StoreEntity>()
                .eq(StoreEntity::getInviteCode, inviteCode)
                .eq(StoreEntity::getDeleted, false));
        
        if (inviterStore == null) {
            return; // Invalid or non-existent invite code
        }

        ReferralRecordEntity record = new ReferralRecordEntity();
        record.setId(IdWorker.getId());
        record.setInviterStoreId(inviterStore.getId());
        record.setInviterUserId(inviterStore.getCreatedBy()); // Assume owner is creator for now
        record.setInviteeStoreId(inviteeStoreId);
        record.setStatus("pending");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        referralRecordMapper.insert(record);
    }

    /**
     * Check and reward both parties when a project is confirmed.
     */
    @Transactional
    public void checkAndReward(Long storeId, Long projectId) {
        ReferralRecordEntity record = referralRecordMapper.selectOne(new LambdaQueryWrapper<ReferralRecordEntity>()
                .eq(ReferralRecordEntity::getInviteeStoreId, storeId)
                .eq(ReferralRecordEntity::getStatus, "pending"));
        
        if (record == null) {
            return;
        }

        // Grant 30 days Pro extension to both inviter and invitee
        extendProPlan(record.getInviterStoreId(), 30);
        extendProPlan(record.getInviteeStoreId(), 30);

        record.setStatus("rewarded");
        record.setRewardedAt(LocalDateTime.now());
        record.setTriggerProjectId(projectId);
        record.setUpdatedAt(LocalDateTime.now());
        referralRecordMapper.updateById(record);

        // Send notifications
        notificationService.create(record.getInviterStoreId(), record.getInviterUserId(), null, 
                "REFERRAL_REWARD", "推荐奖励发放", "您推荐的用户已完成首次审稿，您的 Pro 套餐已延长 30 天。");
    }

    private void extendProPlan(Long storeId, int days) {
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpiresAt = store.getPlanExpiresAt();
        
        if (currentExpiresAt == null || currentExpiresAt.isBefore(now)) {
            store.setPlanExpiresAt(now.plusDays(days));
        } else {
            store.setPlanExpiresAt(currentExpiresAt.plusDays(days));
        }
        
        store.setPlanType("pro");
        store.setUpdatedAt(now);
        storeMapper.updateById(store);
    }
}
