package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.util.PageUtil;
import com.citytravel.citytravel.entity.Favorite;
import com.citytravel.citytravel.entity.Route;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.FavoriteService;
import com.citytravel.citytravel.service.RouteService;
import com.citytravel.citytravel.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/route")
public class RouteController {
    
    @Autowired
    private RouteService routeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @GetMapping("/list")
    public Result<List<Route>> list(@RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Route::getCategoryId, categoryId);
        }
        return Result.success(routeService.list(wrapper));
    }
    
    @GetMapping("/page")
    public Result<IPage<Route>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Page<Route> page = new Page<>(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Route::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Route::getName, keyword)
                    .or().like(Route::getDescription, keyword)
                    .or().like(Route::getLocation, keyword));
        }
        IPage<Route> result = routeService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/search")
    public Result<List<Route>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Route::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Route::getName, keyword)
                    .or().like(Route::getDescription, keyword)
                    .or().like(Route::getLocation, keyword));
        }
        return Result.success(routeService.list(wrapper));
    }
    
    @GetMapping("/{id}")
    public Result<Route> getById(@PathVariable Long id,
                                 @SessionAttribute(name = "user", required = false) User user) {
        Route route = routeService.getById(id);
        if (route == null || (!Integer.valueOf(1).equals(route.getStatus())
                && (user == null || (!UserService.ROLE_ADMIN.equals(user.getRole())
                && !java.util.Objects.equals(route.getMerchantId(), user.getId()))))) {
            return Result.error(404, "路线不存在");
        }
        return Result.success(route);
    }
    
    @PostMapping
    public Result<Route> save(@RequestBody Route route, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        if (!UserService.ROLE_MERCHANT.equals(user.getRole()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("商家才能发布路线");
        }
        route.setMerchantId(user.getId());
        route.setMerchantName(user.getUsername());
        route.setStatus(0);
        route.setCreateTime(LocalDateTime.now());
        if (route.getPrice() == null) {
            route.setPrice(BigDecimal.ZERO);
        }
        routeService.save(route);
        return Result.success(route);
    }
    
    @PutMapping
    public Result<Route> update(@RequestBody Route route, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Route existing = routeService.getById(route.getId());
        if (existing == null) {
            return Result.error("路线不存在");
        }
        if (!UserService.ROLE_ADMIN.equals(user.getRole()) && !java.util.Objects.equals(existing.getMerchantId(), user.getId())) {
            return Result.error("无权限修改");
        }
        route.setId(existing.getId());
        route.setMerchantId(existing.getMerchantId());
        route.setMerchantName(existing.getMerchantName());
        route.setStatus(existing.getStatus());
        route.setCreateTime(existing.getCreateTime());
        route.setUpdateTime(LocalDateTime.now());
        routeService.updateById(route);
        return Result.success(route);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Route route = routeService.getById(id);
        if (route == null) {
            return Result.error("路线不存在");
        }
        if (!UserService.ROLE_ADMIN.equals(user.getRole()) && !route.getMerchantId().equals(user.getId())) {
            return Result.error("无权限删除");
        }
        routeService.removeById(id);
        return Result.success();
    }
    
    @GetMapping("/merchant/list")
    public Result<List<Route>> getMerchantRoutes(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getMerchantId, user.getId());
        return Result.success(routeService.list(wrapper));
    }
    
    @GetMapping("/admin/pending")
    public Result<List<Route>> getPendingRoutes(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getStatus, 0);
        return Result.success(routeService.list(wrapper));
    }
    
    @PutMapping("/admin/{id}/approve")
    public Result<Route> approveRoute(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
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
    
    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getType, "route")
               .eq(Favorite::getRefId, id);
        
        List<Favorite> existing = favoriteService.list(wrapper);
        if (!existing.isEmpty()) {
            favoriteService.removeById(existing.get(0).getId());
            return Result.success("取消收藏成功");
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(user.getId());
        favorite.setStrategyId(null);
        favorite.setType("route");
        favorite.setRefId(id);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteService.save(favorite);
        return Result.success("收藏成功");
    }
    
    @GetMapping("/{id}/favorite/check")
    public Result<Boolean> checkFavorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.success(false);
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getType, "route")
               .eq(Favorite::getRefId, id);
        
        return Result.success(favoriteService.count(wrapper) > 0);
    }
}
