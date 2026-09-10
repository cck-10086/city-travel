package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Attraction;
import com.citytravel.citytravel.entity.Order;
import com.citytravel.citytravel.entity.Strategy;
import com.citytravel.citytravel.service.AttractionService;
import com.citytravel.citytravel.service.OrderService;
import com.citytravel.citytravel.service.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private StrategyService strategyService;
    
    @Autowired
    private AttractionService attractionService;
    
    @Autowired
    private com.citytravel.citytravel.service.UserService userService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> result = new HashMap<>();
        
        long totalOrders = orderService.count();
        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
        
        List<Order> paidOrders = orderService.list(new LambdaQueryWrapper<Order>().eq(Order::getStatus, "paid"));
        for (Order order : paidOrders) {
            if (order.getTotalPrice() != null) {
                totalRevenue = totalRevenue.add(order.getTotalPrice());
            }
        }
        
        long totalUsers = userService.count();
        long totalAttractions = attractionService.count();
        
        result.put("totalOrders", totalOrders);
        result.put("totalRevenue", totalRevenue);
        result.put("totalUsers", totalUsers);
        result.put("totalAttractions", totalAttractions);
        
        return Result.success(result);
    }

    @GetMapping("/order-by-status")
    public Result<List<Map<String, Object>>> getOrdersByStatus() {
        List<Map<String, Object>> data = new ArrayList<>();
        
        String[] statuses = {"unpaid", "paid", "used", "expired", "evaluated"};
        String[] labels = {"待支付", "已支付", "已使用", "已过期", "已评价"};
        
        for (int i = 0; i < statuses.length; i++) {
            long count = orderService.count(new LambdaQueryWrapper<Order>().eq(Order::getStatus, statuses[i]));
            Map<String, Object> item = new HashMap<>();
            item.put("status", statuses[i]);
            item.put("label", labels[i]);
            item.put("count", count);
            data.add(item);
        }
        
        return Result.success(data);
    }

    @GetMapping("/hot-attractions")
    public Result<List<Map<String, Object>>> getHotAttractions() {
        List<Map<String, Object>> hotList = new ArrayList<>();
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getProductType, "attraction");
        List<Order> orders = orderService.list(wrapper);
        
        Map<Long, Long> attractionOrderCount = new HashMap<>();
        for (Order order : orders) {
            attractionOrderCount.merge(order.getProductId(), order.getQuantity().longValue(), Long::sum);
        }
        
        Map<Long, String> attractionNames = new HashMap<>();
        for (Long id : attractionOrderCount.keySet()) {
            Attraction attraction = attractionService.getById(id);
            if (attraction != null) {
                attractionNames.put(id, attraction.getName());
            }
        }
        
        attractionOrderCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("attractionId", entry.getKey());
                    item.put("name", attractionNames.getOrDefault(entry.getKey(), "景点" + entry.getKey()));
                    item.put("orderCount", entry.getValue());
                    hotList.add(item);
                });
        
        return Result.success(hotList);
    }

    @GetMapping("/hot-strategies")
    public Result<List<Map<String, Object>>> getHotStrategies() {
        List<Map<String, Object>> hotList = new ArrayList<>();
        
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getStatus, 1);
        List<Strategy> strategies = strategyService.list(wrapper);
        
        for (Strategy strategy : strategies) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", strategy.getId());
            item.put("title", strategy.getTitle());
            item.put("authorName", strategy.getAuthorName());
            item.put("views", strategy.getViews());
            item.put("likes", strategy.getLikes());
            item.put("image", strategy.getImage());
            hotList.add(item);
        }
        
        hotList.sort((a, b) -> {
            Integer viewsA = (Integer) a.get("views");
            Integer viewsB = (Integer) b.get("views");
            if (viewsA == null) viewsA = 0;
            if (viewsB == null) viewsB = 0;
            return viewsB.compareTo(viewsA);
        });
        
        if (hotList.size() > 10) {
            hotList = hotList.subList(0, 10);
        }
        
        return Result.success(hotList);
    }

    @GetMapping("/order-stats")
    public Result<Map<String, Object>> getOrderStats() {
        Map<String, Object> result = new HashMap<>();
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        long totalOrders = orderService.count(wrapper);
        
        wrapper.eq(Order::getStatus, "paid");
        long paidOrders = orderService.count(wrapper);
        
        wrapper.clear();
        wrapper.eq(Order::getStatus, "unpaid");
        long unpaidOrders = orderService.count(wrapper);
        
        wrapper.clear();
        wrapper.eq(Order::getStatus, "used");
        long usedOrders = orderService.count(wrapper);
        
        result.put("totalOrders", totalOrders);
        result.put("paidOrders", paidOrders);
        result.put("unpaidOrders", unpaidOrders);
        result.put("usedOrders", usedOrders);
        
        return Result.success(result);
    }

    @GetMapping("/order-by-type")
    public Result<List<Map<String, Object>>> getOrdersByType() {
        List<Map<String, Object>> data = new ArrayList<>();
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getProductType, "attraction");
        long attractionOrders = orderService.count(wrapper);
        
        wrapper.clear();
        wrapper.eq(Order::getProductType, "homestay");
        long homestayOrders = orderService.count(wrapper);
        
        wrapper.clear();
        wrapper.eq(Order::getProductType, "route");
        long routeOrders = orderService.count(wrapper);
        
        data.add(mapOf("type", "景点", "value", attractionOrders));
        data.add(mapOf("type", "民宿", "value", homestayOrders));
        data.add(mapOf("type", "线路", "value", routeOrders));
        
        return Result.success(data);
    }

    private Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}