package com.lyllink.proofly.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lyllink.proofly.common.BusinessException;
import com.lyllink.proofly.dao.ProjectMapper;
import com.lyllink.proofly.dao.StoreMapper;
import com.lyllink.proofly.dao.UserMapper;
import com.lyllink.proofly.entity.ProjectEntity;
import com.lyllink.proofly.entity.StoreEntity;
import com.lyllink.proofly.entity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class UsageService {

    private final StoreMapper storeMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    public UsageService(StoreMapper storeMapper, ProjectMapper projectMapper, UserMapper userMapper) {
        this.storeMapper = storeMapper;
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
    }

    public void checkProjectLimit(Long storeId) {
        StoreEntity store = requiredStore(storeId);
        if ("pro".equals(store.getPlanType())) {
            return;
        }

        // 免费版限制：3 个活跃项目
        long count = projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStoreId, storeId)
                .eq(ProjectEntity::getDeleted, false)
                .ne(ProjectEntity::getStatus, "archived"));

        if (count >= 3) {
            throw BusinessException.forbidden("免费版最多只能创建 3 个活跃项目，请升级高级版或归档旧项目");
        }
    }

    public void checkStaffLimit(Long storeId) {
        StoreEntity store = requiredStore(storeId);
        if ("pro".equals(store.getPlanType())) {
            return;
        }

        // 免费版限制：1 名员工（仅限老板）
        long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getStoreId, storeId)
                .eq(UserEntity::getDeleted, false));

        if (count >= 1) {
            throw BusinessException.forbidden("免费版仅限 1 人（老板）使用，添加员工请升级高级版");
        }
    }

    private StoreEntity requiredStore(Long storeId) {
        StoreEntity store = storeMapper.selectById(storeId);
        if (store == null || Boolean.TRUE.equals(store.getDeleted())) {
            throw BusinessException.notFound("门店不存在");
        }
        return store;
    }
}
