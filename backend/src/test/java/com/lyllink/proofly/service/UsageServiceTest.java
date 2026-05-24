package com.lyllink.proofly.service;

import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.entity.StoreEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UsageServiceTest {

    private UsageService usageService;

    @Mock
    private StoreMapper storeMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usageService = new UsageService(storeMapper, projectMapper, userMapper);
    }

    @Test
    void checkProjectLimit_ProPlan_ShouldPass() {
        StoreEntity store = new StoreEntity();
        store.setPlanType("pro");
        when(storeMapper.selectById(1L)).thenReturn(store);

        assertDoesNotThrow(() -> usageService.checkProjectLimit(1L));
    }

    @Test
    void checkProjectLimit_FreePlan_UnderLimit_ShouldPass() {
        StoreEntity store = new StoreEntity();
        store.setPlanType("free");
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(projectMapper.selectCount(any())).thenReturn(2L);

        assertDoesNotThrow(() -> usageService.checkProjectLimit(1L));
    }

    @Test
    void checkProjectLimit_FreePlan_AtLimit_ShouldThrow() {
        StoreEntity store = new StoreEntity();
        store.setPlanType("free");
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(projectMapper.selectCount(any())).thenReturn(3L);

        assertThrows(BusinessException.class, () -> usageService.checkProjectLimit(1L));
    }

    @Test
    void checkStaffLimit_FreePlan_AtLimit_ShouldThrow() {
        StoreEntity store = new StoreEntity();
        store.setPlanType("free");
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usageService.checkStaffLimit(1L));
    }
}
