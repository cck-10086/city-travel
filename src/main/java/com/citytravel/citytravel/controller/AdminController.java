package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.*;
import com.citytravel.citytravel.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.citytravel.citytravel.util.OrderStatusUtil;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private HomestayService homestayService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        Map<String, Object> dashboard = new HashMap<>();
        
        long totalUsers = userService.count();
        long totalTourists = userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, UserService.ROLE_TOURIST));
        long totalMerchants = userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, UserService.ROLE_MERCHANT));
        long pendingMerchants = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserService.ROLE_MERCHANT)
                .eq(User::getStatus, 0));
        
        long totalAttractions = attractionService.count();
        long totalHomestays = homestayService.count();
        long totalRoutes = routeService.count();
        long totalStrategies = strategyService.count();
        long totalOrders = orderService.count();

        long pendingAttractions = attractionService.count(new LambdaQueryWrapper<Attraction>().eq(Attraction::getStatus, 0));
        long pendingHomestays = homestayService.count(new LambdaQueryWrapper<Homestay>().eq(Homestay::getStatus, 0));
        long pendingRoutes = routeService.count(new LambdaQueryWrapper<Route>().eq(Route::getStatus, 0));
        long pendingStrategies = strategyService.count(new LambdaQueryWrapper<Strategy>().eq(Strategy::getStatus, 0));
        long pendingContents = pendingAttractions + pendingHomestays + pendingRoutes + pendingStrategies;

        dashboard.put("totalUsers", totalUsers);
        dashboard.put("totalTourists", totalTourists);
        dashboard.put("totalMerchants", totalMerchants);
        dashboard.put("pendingMerchants", pendingMerchants);
        dashboard.put("totalAttractions", totalAttractions);
        dashboard.put("totalHomestays", totalHomestays);
        dashboard.put("totalRoutes", totalRoutes);
        dashboard.put("totalStrategies", totalStrategies);
        dashboard.put("totalOrders", totalOrders);
        dashboard.put("pendingContents", pendingContents);

        return Result.success(dashboard);
    }

    @GetMapping("/users")
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<User> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> userPage = userService.page(pageRequest, wrapper);
        
        return Result.success(userPage);
    }

    @GetMapping("/users/{id}")
    public Result<User> getUserById(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        User user = userService.getById(id);
        if (user != null) {
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    @PutMapping("/users/{id}")
    public Result<User> updateUser(
            @PathVariable Long id,
            @RequestBody User userData,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        User user = userService.getById(id);
        if (user != null) {
            if (userData.getUsername() != null) user.setUsername(userData.getUsername());
            if (userData.getEmail() != null) user.setEmail(userData.getEmail());
            if (userData.getPhone() != null) user.setPhone(userData.getPhone());
            if (userData.getRole() != null) user.setRole(userData.getRole());
            if (userData.getStatus() != null) user.setStatus(userData.getStatus());
            userService.updateById(user);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        userService.removeById(id);
        return Result.success();
    }

    @GetMapping("/merchants")
    public Result<IPage<User>> getMerchantList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<User> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, UserService.ROLE_MERCHANT);
        
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> merchantPage = userService.page(pageRequest, wrapper);
        
        return Result.success(merchantPage);
    }

    @PutMapping("/merchants/{id}/approve")
    public Result<User> approveMerchant(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        User merchant = userService.getById(id);
        if (merchant != null) {
            merchant.setStatus(1);
            userService.updateById(merchant);
            return Result.success(merchant);
        }
        return Result.error("商家不存在");
    }

    @PutMapping("/merchants/{id}/reject")
    public Result<User> rejectMerchant(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        User merchant = userService.getById(id);
        if (merchant != null) {
            merchant.setStatus(0);
            userService.updateById(merchant);
            return Result.success(merchant);
        }
        return Result.error("商家不存在");
    }

    @DeleteMapping("/merchants/{id}")
    public Result<Void> deleteMerchant(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        userService.removeById(id);
        return Result.success();
    }

    @GetMapping("/attractions")
    public Result<IPage<Attraction>> getAttractionList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<Attraction> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            wrapper.like(Attraction::getName, name);
        }
        
        wrapper.orderByDesc(Attraction::getCreateTime);
        IPage<Attraction> attractionPage = attractionService.page(pageRequest, wrapper);
        
        return Result.success(attractionPage);
    }

    @PutMapping("/attractions/{id}")
    public Result<Attraction> updateAttraction(
            @PathVariable Long id,
            @RequestBody Attraction attractionData,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Attraction attraction = attractionService.getById(id);
        if (attraction != null) {
            if (attractionData.getName() != null) attraction.setName(attractionData.getName());
            if (attractionData.getDescription() != null) attraction.setDescription(attractionData.getDescription());
            if (attractionData.getLocation() != null) attraction.setLocation(attractionData.getLocation());
            if (attractionData.getPrice() != null) attraction.setPrice(attractionData.getPrice());
            if (attractionData.getStock() != null) attraction.setStock(attractionData.getStock());
            if (attractionData.getImage() != null) attraction.setImage(attractionData.getImage());
            if (attractionData.getStatus() != null) attraction.setStatus(attractionData.getStatus());
            attractionService.updateById(attraction);
            return Result.success(attraction);
        }
        return Result.error("景点不存在");
    }

    @PutMapping("/attractions/{id}/approve")
    public Result<Attraction> approveAttraction(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        Attraction attraction = attractionService.getById(id);
        if (attraction != null) {
            attraction.setStatus(1);
            attractionService.updateById(attraction);
            return Result.success(attraction);
        }
        return Result.error("景点不存在");
    }

    @DeleteMapping("/attractions/{id}")
    public Result<Void> deleteAttraction(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        attractionService.removeById(id);
        return Result.success();
    }

    @GetMapping("/homestays")
    public Result<IPage<Homestay>> getHomestayList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<Homestay> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<Homestay> wrapper = new LambdaQueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            wrapper.like(Homestay::getName, name);
        }
        
        wrapper.orderByDesc(Homestay::getCreateTime);
        IPage<Homestay> homestayPage = homestayService.page(pageRequest, wrapper);
        
        return Result.success(homestayPage);
    }

    @PutMapping("/homestays/{id}")
    public Result<Homestay> updateHomestay(
            @PathVariable Long id,
            @RequestBody Homestay homestayData,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Homestay homestay = homestayService.getById(id);
        if (homestay != null) {
            if (homestayData.getName() != null) homestay.setName(homestayData.getName());
            if (homestayData.getDescription() != null) homestay.setDescription(homestayData.getDescription());
            if (homestayData.getLocation() != null) homestay.setLocation(homestayData.getLocation());
            if (homestayData.getPrice() != null) homestay.setPrice(homestayData.getPrice());
            if (homestayData.getStock() != null) homestay.setStock(homestayData.getStock());
            if (homestayData.getImage() != null) homestay.setImage(homestayData.getImage());
            if (homestayData.getStatus() != null) homestay.setStatus(homestayData.getStatus());
            homestayService.updateById(homestay);
            return Result.success(homestay);
        }
        return Result.error("民宿不存在");
    }

    @PutMapping("/homestays/{id}/approve")
    public Result<Homestay> approveHomestay(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        Homestay homestay = homestayService.getById(id);
        if (homestay != null) {
            homestay.setStatus(1);
            homestayService.updateById(homestay);
            return Result.success(homestay);
        }
        return Result.error("民宿不存在");
    }

    @DeleteMapping("/homestays/{id}")
    public Result<Void> deleteHomestay(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        homestayService.removeById(id);
        return Result.success();
    }

    @GetMapping("/routes")
    public Result<IPage<Route>> getRouteList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<Route> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            wrapper.like(Route::getName, name);
        }
        
        wrapper.orderByDesc(Route::getCreateTime);
        IPage<Route> routePage = routeService.page(pageRequest, wrapper);
        
        return Result.success(routePage);
    }

    @PutMapping("/routes/{id}")
    public Result<Route> updateRoute(
            @PathVariable Long id,
            @RequestBody Route routeData,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Route route = routeService.getById(id);
        if (route != null) {
            if (routeData.getName() != null) route.setName(routeData.getName());
            if (routeData.getDescription() != null) route.setDescription(routeData.getDescription());
            if (routeData.getLocation() != null) route.setLocation(routeData.getLocation());
            if (routeData.getPrice() != null) route.setPrice(routeData.getPrice());
            if (routeData.getDays() != null) route.setDays(routeData.getDays());
            if (routeData.getImage() != null) route.setImage(routeData.getImage());
            if (routeData.getStatus() != null) route.setStatus(routeData.getStatus());
            routeService.updateById(route);
            return Result.success(route);
        }
        return Result.error("路线不存在");
    }

    @PutMapping("/routes/{id}/approve")
    public Result<Route> approveRoute(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        Route route = routeService.getById(id);
        if (route != null) {
            route.setStatus(1);
            routeService.updateById(route);
            return Result.success(route);
        }
        return Result.error("路线不存在");
    }

    @DeleteMapping("/routes/{id}")
    public Result<Void> deleteRoute(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        routeService.removeById(id);
        return Result.success();
    }

    @GetMapping("/strategies")
    public Result<IPage<Strategy>> getStrategyList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<Strategy> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        
        if (title != null && !title.isEmpty()) {
            wrapper.like(Strategy::getTitle, title);
        }
        
        wrapper.orderByDesc(Strategy::getCreateTime);
        IPage<Strategy> strategyPage = strategyService.page(pageRequest, wrapper);
        
        return Result.success(strategyPage);
    }

    @PutMapping("/strategies/{id}")
    public Result<Strategy> updateStrategy(
            @PathVariable Long id,
            @RequestBody Strategy strategyData,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Strategy strategy = strategyService.getById(id);
        if (strategy != null) {
            if (strategyData.getTitle() != null) strategy.setTitle(strategyData.getTitle());
            if (strategyData.getContent() != null) strategy.setContent(strategyData.getContent());
            if (strategyData.getImage() != null) strategy.setImage(strategyData.getImage());
            if (strategyData.getStatus() != null) strategy.setStatus(strategyData.getStatus());
            strategyService.updateById(strategy);
            return Result.success(strategy);
        }
        return Result.error("攻略不存在");
    }

    @PutMapping("/strategies/{id}/approve")
    public Result<Strategy> approveStrategy(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        Strategy strategy = strategyService.getById(id);
        if (strategy != null) {
            strategy.setStatus(1);
            strategyService.updateById(strategy);
            return Result.success(strategy);
        }
        return Result.error("攻略不存在");
    }

    @DeleteMapping("/strategies/{id}")
    public Result<Void> deleteStrategy(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        strategyService.removeById(id);
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<IPage<Order>> getOrderList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<Order> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = orderService.page(pageRequest, wrapper);
        
        return Result.success(orderPage);
    }

    @PutMapping("/orders/{id}")
    public Result<Order> updateOrder(
            @PathVariable Long id,
            @RequestBody Order orderData,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Order order = orderService.getById(id);
        if (order != null) {
            if (orderData.getStatus() != null) order.setStatus(orderData.getStatus());
            orderService.updateById(order);
            return Result.success(order);
        }
        return Result.error("订单不存在");
    }

    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        orderService.removeById(id);
        return Result.success();
    }

    @GetMapping("/orders/export")
    public void exportOrders(HttpSession session, HttpServletResponse response) throws IOException {
        if (!isAdmin(session)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("无权限");
            return;
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderService.list(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        String fileName = java.net.URLEncoder.encode("订单列表_" + java.time.LocalDate.now() + ".csv", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);

        try (PrintWriter writer = response.getWriter()) {
            writer.println("订单号,商品名称,商品类型,数量,总价,状态,下单时间,支付时间,用户ID");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Order order : orders) {
                writer.println(String.format("%s,%s,%s,%d,%.2f,%s,%s,%s,%d",
                        escapeCsvField(order.getOrderNo()),
                        escapeCsvField(order.getProductName()),
                        escapeCsvField(order.getProductType()),
                        order.getQuantity(),
                        order.getTotalPrice(),
                        OrderStatusUtil.getStatusText(order.getStatus()),
                        order.getCreateTime() != null ? order.getCreateTime().format(formatter) : "",
                        order.getPayTime() != null ? order.getPayTime().format(formatter) : "",
                        order.getUserId()
                ));
            }
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    @PutMapping("/strategies/batch-approve")
    public Result<Void> batchApproveStrategies(@RequestBody List<Long> ids, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        for (Long id : ids) {
            Strategy strategy = strategyService.getById(id);
            if (strategy != null) {
                strategy.setStatus(1);
                strategyService.updateById(strategy);
            }
        }
        return Result.success("批量审核通过成功");
    }

    @PutMapping("/strategies/batch-reject")
    public Result<Void> batchRejectStrategies(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        String rejectReason = (String) request.get("reason");
        
        for (Long id : ids) {
            Strategy strategy = strategyService.getById(id);
            if (strategy != null) {
                strategy.setStatus(-1);
                strategyService.updateById(strategy);
            }
        }
        return Result.success("批量拒绝成功");
    }

    @GetMapping("/strategies/pending")
    public Result<IPage<Strategy>> getPendingStrategies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error("无权限");
        }
        
        Page<Strategy> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getStatus, 0);
        wrapper.orderByDesc(Strategy::getCreateTime);
        
        return Result.success(strategyService.page(pageRequest, wrapper));
    }
}