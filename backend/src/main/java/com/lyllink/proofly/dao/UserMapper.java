package com.lyllink.proofly.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyllink.proofly.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
