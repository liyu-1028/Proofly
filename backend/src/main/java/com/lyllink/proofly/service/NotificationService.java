package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.dao.NotificationMapper;
import com.lyllink.proofly.dto.resp.NotificationResponse;
import com.lyllink.proofly.entity.NotificationEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void create(Long storeId, Long receiverId, Long projectId, String type, String title, String content) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(IdWorker.getId());
        entity.setStoreId(storeId);
        entity.setReceiverUserId(receiverId);
        entity.setProjectId(projectId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(false);
        notificationMapper.insert(entity);
    }

    public List<NotificationResponse> list(Long storeId, Long receiverId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getStoreId, storeId)
                .eq(NotificationEntity::getReceiverUserId, receiverId)
                .eq(NotificationEntity::getDeleted, false)
                .orderByDesc(NotificationEntity::getCreatedAt)
                .last("LIMIT 50")) // 限制显示最近 50 条
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(Long storeId, Long receiverId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getStoreId, storeId)
                .eq(NotificationEntity::getReceiverUserId, receiverId)
                .isNull(NotificationEntity::getReadAt)
                .eq(NotificationEntity::getDeleted, false));
    }

    @Transactional
    public void markAsRead(Long storeId, Long receiverId, Long notificationId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .set(NotificationEntity::getReadAt, LocalDateTime.now())
                .eq(NotificationEntity::getStoreId, storeId)
                .eq(NotificationEntity::getReceiverUserId, receiverId)
                .eq(NotificationEntity::getId, notificationId)
                .isNull(NotificationEntity::getReadAt));
    }

    @Transactional
    public void markAllAsRead(Long storeId, Long receiverId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .set(NotificationEntity::getReadAt, LocalDateTime.now())
                .eq(NotificationEntity::getStoreId, storeId)
                .eq(NotificationEntity::getReceiverUserId, receiverId)
                .isNull(NotificationEntity::getReadAt)
                .eq(NotificationEntity::getDeleted, false));
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        NotificationResponse resp = new NotificationResponse();
        resp.setId(entity.getId());
        resp.setReceiverUserId(entity.getReceiverUserId());
        resp.setProjectId(entity.getProjectId());
        resp.setType(entity.getType());
        resp.setTitle(entity.getTitle());
        resp.setContent(entity.getContent());
        resp.setReadAt(entity.getReadAt());
        resp.setCreatedAt(entity.getCreatedAt());
        return resp;
    }
}
