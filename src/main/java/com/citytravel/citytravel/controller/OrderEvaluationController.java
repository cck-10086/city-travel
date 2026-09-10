package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Order;
import com.citytravel.citytravel.entity.OrderEvaluation;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.OrderEvaluationService;
import com.citytravel.citytravel.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluation")
public class OrderEvaluationController {
    
    @Autowired
    private OrderEvaluationService evaluationService;
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public Result<OrderEvaluation> create(@RequestBody Map<String, Object> data, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        Long orderId = Long.parseLong(data.get("orderId").toString());
        String content = (String) data.get("content");
        Integer rating = (Integer) data.get("rating");
        
        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!order.getUserId().equals(((User) user).getId())) {
            return Result.error("只能评价自己的订单");
        }
        
        if (!"used".equals(order.getStatus())) {
            return Result.error("只有已使用的订单才能评价");
        }
        
        LambdaQueryWrapper<OrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderEvaluation::getOrderId, orderId);
        if (evaluationService.count(wrapper) > 0) {
            return Result.error("该订单已评价过");
        }
        
        OrderEvaluation evaluation = evaluationService.create(orderId, ((User) user).getId(), content, rating);
        order.setStatus("evaluated");
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);
        
        return Result.success(evaluation);
    }
    
    @GetMapping("/order/{orderId}")
    public Result<OrderEvaluation> getByOrderId(@PathVariable Long orderId) {
        LambdaQueryWrapper<OrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderEvaluation::getOrderId, orderId);
        OrderEvaluation evaluation = evaluationService.getOne(wrapper);
        return Result.success(evaluation);
    }
    
    @GetMapping("/user")
    public Result<List<OrderEvaluation>> getMyEvaluations(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        LambdaQueryWrapper<OrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderEvaluation::getUserId, ((User) user).getId());
        wrapper.orderByDesc(OrderEvaluation::getCreateTime);
        return Result.success(evaluationService.list(wrapper));
    }
}