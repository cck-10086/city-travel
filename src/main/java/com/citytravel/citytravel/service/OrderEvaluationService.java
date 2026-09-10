package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.OrderEvaluation;
import com.citytravel.citytravel.mapper.OrderEvaluationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderEvaluationService extends ServiceImpl<OrderEvaluationMapper, OrderEvaluation> {
    
    public OrderEvaluation create(Long orderId, Long userId, String content, Integer rating) {
        OrderEvaluation evaluation = new OrderEvaluation();
        evaluation.setOrderId(orderId);
        evaluation.setUserId(userId);
        evaluation.setContent(content);
        evaluation.setRating(rating);
        evaluation.setCreateTime(LocalDateTime.now());
        save(evaluation);
        return evaluation;
    }
}