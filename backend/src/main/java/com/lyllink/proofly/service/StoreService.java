package com.lyllink.proofly.service;

import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.dto.req.StoreUpdateRequest;
import com.lyllink.proofly.dto.resp.StoreResponse;
import com.lyllink.proofly.entity.StoreEntity;
import com.lyllink.proofly.security.CurrentUser;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {

    private final StoreMapper storeMapper;

    public StoreService(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    public StoreResponse current(CurrentUser currentUser) {
        return toResponse(requiredStore(currentUser.storeId()));
    }

    @Transactional
    public StoreResponse updateCurrent(CurrentUser currentUser, StoreUpdateRequest request) {
        requireOwnerOrAdmin(currentUser);
        StoreEntity store = requiredStore(currentUser.storeId());
        store.setName(request.name().trim());
        store.setContactName(trimToNull(request.contactName()));
        store.setContactPhone(trimToNull(request.contactPhone()));
        store.setUpdatedBy(currentUser.userId());
        store.setUpdatedAt(LocalDateTime.now());
        storeMapper.updateById(store);
        return toResponse(requiredStore(currentUser.storeId()));
    }

    private StoreEntity requiredStore(Long storeId) {
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null || Boolean.TRUE.equals(store.getDeleted())) {
            throw BusinessException.notFound("门店不存在");
        }
        return store;
    }

    private void requireOwnerOrAdmin(CurrentUser currentUser) {
        if (!currentUser.roles().contains("owner") && !currentUser.roles().contains("admin")) {
            throw BusinessException.forbidden("当前账号无门店管理权限");
        }
    }

    private StoreResponse toResponse(StoreEntity store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getContactName(),
                store.getContactPhone(),
                store.getStatus(),
                store.getDeploymentMode(),
                store.getPlanType(),
                store.getPlanExpiresAt(),
                store.getInviteCode(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
