package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Order;
import com.citytravel.citytravel.mapper.OrderMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {
    
    public String generateOrderNo() {
        return "ORD" + System.currentTimeMillis();
    }
}
