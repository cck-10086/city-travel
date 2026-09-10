package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.*;
import com.citytravel.citytravel.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import com.citytravel.citytravel.util.OrderStatusUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController extends BaseController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AttractionService attractionService;
    
    @Autowired
    private HomestayService homestayService;
    
    @Autowired
    private RouteService routeService;
    
    private boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session) != null;
    }
    
    @PostMapping
    public Result<Order> create(@RequestBody Order order, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        String validationError = normalizeOrderProduct(order);
        if (validationError != null) {
            return Result.error(400, validationError);
        }
        order.setUserId(currentUser.getId());
        order.setOrderNo(orderService.generateOrderNo());
        order.setStatus("unpaid");
        order.setCreateTime(LocalDateTime.now());
        orderService.save(order);
        return Result.success(order);
    }
    
    @PostMapping("/pay/{id}")
    public Result<Order> pay(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(currentUser.getId())) {
            return Result.error("无权限支付此订单");
        }
        if ("paid".equals(order.getStatus())) {
            return Result.error("订单已支付");
        }
        order.setStatus("paid");
        order.setPayTime(LocalDateTime.now());
        orderService.updateById(order);
        return Result.success(order);
    }
    
    @GetMapping("/my")
    public Result<List<Order>> myOrders(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, currentUser.getId());
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderService.list(wrapper));
    }
    
    @GetMapping("/list")
    public Result<List<Order>> list(HttpSession session) {
        if (!isAdmin(session)) {
            return Result.error(403, "无权限");
        }
        return Result.success(orderService.list());
    }
    
    @GetMapping("/admin/list")
    public Result<List<Order>> adminList(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            return Result.error("无权限");
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderService.list(wrapper));
    }
    
    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error(401, "请先登录");
        }
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!isAdmin(session) && !currentUser.getId().equals(order.getUserId())
                && !isMerchantOrderOwner(currentUser, order)) {
            return Result.error(403, "无权限查看此订单");
        }
        return Result.success(order);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        Order existing = orderService.getById(id);
        if (existing == null) {
            return Result.error("订单不存在");
        }
        
        if (!existing.getUserId().equals(currentUser.getId()) && !"admin".equals(currentUser.getRole())) {
            return Result.error("只能取消自己的订单");
        }
        
        if ("paid".equals(existing.getStatus())) {
            return Result.error("已支付的订单无法取消");
        }
        
        orderService.removeById(id);
        return Result.success();
    }
    
    @GetMapping("/export")
    public void export(HttpSession session, HttpServletResponse response) throws IOException {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            response.getWriter().write("请先登录");
            return;
        }
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, currentUser.getId());
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderService.list(wrapper);
        
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=orders.csv");
        
        PrintWriter writer = response.getWriter();
        writer.println("订单号,商品名称,商品类型,数量,总价,状态,下单时间");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Order order : orders) {
            writer.println(String.format("%s,%s,%s,%d,%.2f,%s,%s",
                    order.getOrderNo(),
                    order.getProductName(),
                    order.getProductType(),
                    order.getQuantity(),
                    order.getTotalPrice(),
                    OrderStatusUtil.getStatusText(order.getStatus()),
                    order.getCreateTime() != null ? order.getCreateTime().format(formatter) : ""
            ));
        }
        
        writer.flush();
        writer.close();
    }
    
    @GetMapping("/merchant")
    public Result<List<Order>> getMerchantOrders(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        if (!"merchant".equals(currentUser.getRole())) {
            return Result.error("只有商家可以查看订单");
        }
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> allOrders = orderService.list(wrapper);
        
        return Result.success(allOrders.stream().filter(order -> {
            String productType = order.getProductType();
            Long productId = order.getProductId();
            
            if ("attraction".equals(productType)) {
                Attraction attraction = attractionService.getById(productId);
                return attraction != null && currentUser.getId().equals(attraction.getMerchantId());
            } else if ("homestay".equals(productType)) {
                Homestay homestay = homestayService.getById(productId);
                return homestay != null && currentUser.getId().equals(homestay.getMerchantId());
            } else if ("route".equals(productType)) {
                Route route = routeService.getById(productId);
                return route != null && currentUser.getId().equals(route.getMerchantId());
            }
            return false;
        }).collect(java.util.stream.Collectors.toList()));
    }
    
    @PostMapping("/{id}/confirm")
    public Result<Order> confirmOrder(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!"merchant".equals(currentUser.getRole()) && !"admin".equals(currentUser.getRole())) {
            return Result.error("只有商家或管理员可以确认订单");
        }
        if (!isAdmin(session) && !isMerchantOrderOwner(currentUser, order)) {
            return Result.error(403, "无权限确认其他商家的订单");
        }
        
        if (!"paid".equals(order.getStatus())) {
            return Result.error("只能确认已支付的订单");
        }
        
        order.setStatus("used");
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);
        return Result.success(order);
    }
    
    @PostMapping("/{id}/expire")
    public Result<Order> expireOrder(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return Result.error("请先登录");
        }
        
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!"admin".equals(currentUser.getRole())) {
            return Result.error("只有管理员可以设置订单过期");
        }
        
        order.setStatus("expired");
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);
        return Result.success(order);
    }

    private String normalizeOrderProduct(Order order) {
        if (order.getProductId() == null || order.getProductType() == null) {
            return "商品信息不完整";
        }
        if (order.getQuantity() == null || order.getQuantity() < 1 || order.getQuantity() > 20) {
            return "预订数量必须在1至20之间";
        }

        String productName;
        BigDecimal unitPrice;
        Integer status;
        if ("attraction".equals(order.getProductType())) {
            Attraction attraction = attractionService.getById(order.getProductId());
            if (attraction == null) return "景点不存在";
            productName = attraction.getName();
            unitPrice = attraction.getPrice();
            status = attraction.getStatus();
            if (attraction.getStock() != null && attraction.getStock() < order.getQuantity()) return "库存不足";
        } else if ("homestay".equals(order.getProductType())) {
            Homestay homestay = homestayService.getById(order.getProductId());
            if (homestay == null) return "民宿不存在";
            productName = homestay.getName();
            unitPrice = homestay.getPrice();
            status = homestay.getStatus();
            if (homestay.getStock() != null && homestay.getStock() < order.getQuantity()) return "库存不足";
        } else if ("route".equals(order.getProductType())) {
            Route route = routeService.getById(order.getProductId());
            if (route == null) return "路线不存在";
            productName = route.getName();
            unitPrice = route.getPrice();
            status = route.getStatus();
        } else {
            return "商品类型无效";
        }
        if (!Integer.valueOf(1).equals(status) || unitPrice == null) {
            return "商品当前不可预订";
        }
        order.setProductName(productName);
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(order.getQuantity())));
        return null;
    }

    private boolean isMerchantOrderOwner(User user, Order order) {
        if (!UserService.ROLE_MERCHANT.equals(user.getRole())) {
            return false;
        }
        Long productId = order.getProductId();
        if ("attraction".equals(order.getProductType())) {
            Attraction attraction = attractionService.getById(productId);
            return attraction != null && user.getId().equals(attraction.getMerchantId());
        }
        if ("homestay".equals(order.getProductType())) {
            Homestay homestay = homestayService.getById(productId);
            return homestay != null && user.getId().equals(homestay.getMerchantId());
        }
        if ("route".equals(order.getProductType())) {
            Route route = routeService.getById(productId);
            return route != null && user.getId().equals(route.getMerchantId());
        }
        return false;
    }
}
