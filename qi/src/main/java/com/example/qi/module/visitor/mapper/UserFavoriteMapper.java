package com.example.qi.module.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qi.module.visitor.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
