package com.citytravel.citytravel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytravel.citytravel.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
