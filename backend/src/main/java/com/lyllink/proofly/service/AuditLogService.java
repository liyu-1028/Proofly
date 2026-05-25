package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.dao.AuditLogMapper;
import com.lyllink.proofly.entity.AuditLogEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录业务操作日志。
     */
    public void log(
            Long storeId,
            String action,
            String targetType,
            Long targetId,
            String operatorType,
            Long operatorId,
            String operatorName,
            String summary,
            String extraJson
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(IdWorker.getId());
        entity.setStoreId(storeId);
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setOperatorType(operatorType);
        entity.setOperatorId(operatorId);
        entity.setOperatorName(operatorName);
        entity.setSummary(summary);
        entity.setExtraJson(extraJson);
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    /**
     * 后台用户的专用日志记录。
     */
    public void logUserAction(CurrentUser user, String action, String targetType, Long targetId, String summary) {
        log(user.storeId(), action, targetType, targetId, "user", user.userId(), user.nickname(), summary, null);
    }

    /**
     * 客户的专用日志记录。
     */
    public void logCustomerAction(Long storeId, String action, String targetType, Long targetId, String customerName, String summary) {
        log(storeId, action, targetType, targetId, "customer", null, customerName, summary, null);
    }

    /**
     * 查询特定项目的时间轴。
     */
    public List<AuditLogEntity> getProjectTimeline(Long storeId, Long projectId) {
        return auditLogMapper.selectList(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getStoreId, storeId)
                .eq(AuditLogEntity::getTargetType, "project")
                .eq(AuditLogEntity::getTargetId, projectId)
                .orderByDesc(AuditLogEntity::getCreatedAt));
    }
}
