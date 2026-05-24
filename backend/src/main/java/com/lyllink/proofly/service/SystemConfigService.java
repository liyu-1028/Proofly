package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lyllink.proofly.dao.SystemConfigMapper;
import com.lyllink.proofly.dto.req.SystemConfigUpdateRequest;
import com.lyllink.proofly.dto.resp.SystemConfigResponse;
import com.lyllink.proofly.entity.SystemConfigEntity;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    public SystemConfigService(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    /**
     * List all configs for a store, merged with global defaults.
     */
    public List<SystemConfigResponse> listMergedConfigs(Long storeId) {
        // 1. Fetch global configs (store_id IS NULL)
        List<SystemConfigEntity> globals = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfigEntity>()
                .isNull(SystemConfigEntity::getStoreId)
                .eq(SystemConfigEntity::getDeleted, false));

        // 2. Fetch store-specific configs
        List<SystemConfigEntity> storeConfigs = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getStoreId, storeId)
                .eq(SystemConfigEntity::getDeleted, false));

        // 3. Merge: store overrides global
        Map<String, SystemConfigEntity> mergedMap = new HashMap<>();
        for (SystemConfigEntity g : globals) {
            mergedMap.put(g.getConfigKey(), g);
        }
        for (SystemConfigEntity s : storeConfigs) {
            mergedMap.put(s.getConfigKey(), s);
        }

        return mergedMap.values().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Update or create a store-specific override for a config key.
     */
    @Transactional
    public SystemConfigResponse updateStoreConfig(Long storeId, String key, SystemConfigUpdateRequest request, Long userId) {
        SystemConfigEntity entity = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getStoreId, storeId)
                .eq(SystemConfigEntity::getConfigKey, key)
                .eq(SystemConfigEntity::getDeleted, false));

        if (entity == null) {
            // Find template from global config to get value_type
            SystemConfigEntity template = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                    .isNull(SystemConfigEntity::getStoreId)
                    .eq(SystemConfigEntity::getConfigKey, key)
                    .eq(SystemConfigEntity::getDeleted, false));

            entity = new SystemConfigEntity();
            entity.setId(IdWorker.getId());
            entity.setStoreId(storeId);
            entity.setConfigKey(key);
            entity.setValueType(template != null ? template.getValueType() : "string");
            entity.setConfigValue(request.getConfigValue());
            entity.setDescription(request.getDescription() != null ? request.getDescription() : (template != null ? template.getDescription() : null));
            entity.setCreatedBy(userId);
            entity.setUpdatedBy(userId);
            entity.setDeleted(false);
            systemConfigMapper.insert(entity);
        } else {
            entity.setConfigValue(request.getConfigValue());
            if (request.getDescription() != null) {
                entity.setDescription(request.getDescription());
            }
            entity.setUpdatedBy(userId);
            entity.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.updateById(entity);
        }

        return toResponse(entity);
    }

    public String getConfigValue(Long storeId, String key, String defaultValue) {
        // Try store specific first
        SystemConfigEntity entity = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .eq(SystemConfigEntity::getStoreId, storeId)
                .eq(SystemConfigEntity::getConfigKey, key)
                .eq(SystemConfigEntity::getDeleted, false));
        
        if (entity != null) {
            return entity.getConfigValue();
        }

        // Try global
        entity = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfigEntity>()
                .isNull(SystemConfigEntity::getStoreId)
                .eq(SystemConfigEntity::getConfigKey, key)
                .eq(SystemConfigEntity::getDeleted, false));
        
        return entity != null ? entity.getConfigValue() : defaultValue;
    }

    public int getConfigInt(Long storeId, String key, int defaultValue) {
        String val = getConfigValue(storeId, key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private SystemConfigResponse toResponse(SystemConfigEntity entity) {
        SystemConfigResponse resp = new SystemConfigResponse();
        resp.setId(entity.getId());
        resp.setStoreId(entity.getStoreId());
        resp.setConfigKey(entity.getConfigKey());
        resp.setConfigValue(entity.getConfigValue());
        resp.setValueType(entity.getValueType());
        resp.setDescription(entity.getDescription());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
