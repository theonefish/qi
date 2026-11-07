package com.example.qi.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qi.module.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
