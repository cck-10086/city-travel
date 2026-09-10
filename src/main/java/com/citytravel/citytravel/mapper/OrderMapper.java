package com.citytravel.citytravel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytravel.citytravel.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
