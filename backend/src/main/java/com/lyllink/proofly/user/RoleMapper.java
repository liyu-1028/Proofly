package com.lyllink.proofly.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {

    @Select("""
            SELECT r.code
            FROM user_role ur
            JOIN role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.store_id = #{storeId}
              AND r.deleted = 0
            ORDER BY r.code
            """)
    List<String> selectRoleCodesByUserId(@Param("storeId") Long storeId, @Param("userId") Long userId);
}
