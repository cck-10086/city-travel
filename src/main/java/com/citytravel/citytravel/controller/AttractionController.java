package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Attraction;
import com.citytravel.citytravel.entity.Favorite;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.AttractionService;
import com.citytravel.citytravel.service.CacheService;
import com.citytravel.citytravel.service.FavoriteService;
import com.citytravel.citytravel.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/attraction")
public class AttractionController extends BaseController {  
    private static final String CACHE_KEY_ATTRACTIONS = "attractions:list";
    
    @Autowired
    private AttractionService attractionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @GetMapping("/list")
    public Result<List<Attraction>> list(@RequestParam(required = false) Long categoryId) {
        @SuppressWarnings("unchecked")
        List<Attraction> cachedAttractions = (List<Attraction>) cacheService.get(CACHE_KEY_ATTRACTIONS);
        if (categoryId == null && cachedAttractions != null && !cachedAttractions.isEmpty()) {
            return Result.success(cachedAttractions);
        }
        
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Attraction::getCategoryId, categoryId);
        }
        List<Attraction> attractions = attractionService.list(wrapper);
        
        if (categoryId == null) {
            cacheService.set(CACHE_KEY_ATTRACTIONS, attractions, 30, TimeUnit.MINUTES);
        }
        return Result.success(attractions);
    }
    
    @GetMapping("/page")
    public Result<IPage<Attraction>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Page<Attraction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Attraction::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Attraction::getName, keyword)
                    .or().like(Attraction::getDescription, keyword)
                    .or().like(Attraction::getLocation, keyword));
        }
        IPage<Attraction> result = attractionService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/search")
    public Result<List<Attraction>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Attraction::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Attraction::getName, keyword)
                    .or().like(Attraction::getDescription, keyword)
                    .or().like(Attraction::getLocation, keyword));
        }
        return Result.success(attractionService.list(wrapper));
    }
    
    @GetMapping("/{id}")
    public Result<Attraction> getById(@PathVariable Long id,
                                      @SessionAttribute(name = "user", required = false) User user) {
        Attraction attraction = attractionService.getById(id);
        if (attraction == null || (!Integer.valueOf(1).equals(attraction.getStatus())
                && (user == null || (!UserService.ROLE_ADMIN.equals(user.getRole())
                && !java.util.Objects.equals(attraction.getMerchantId(), user.getId()))))) {
            return Result.error(404, "景点不存在");
        }
        return Result.success(attraction);
    }
    
    @PostMapping
    public Result<Attraction> save(@RequestBody Attraction attraction, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        if (!UserService.ROLE_MERCHANT.equals(user.getRole()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("商家才能发布景点");
        }
        attraction.setMerchantId(user.getId());
        attraction.setMerchantName(user.getUsername());
        attraction.setStatus(0);
        attraction.setCreateTime(LocalDateTime.now());
        attractionService.save(attraction);
        cacheService.delete(CACHE_KEY_ATTRACTIONS);
        return Result.success(attraction);
    }
    
    @PutMapping
    public Result<Attraction> update(@RequestBody Attraction attraction, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Attraction existing = attractionService.getById(attraction.getId());
        if (existing == null) {
            return Result.error("景点不存在");
        }
        if (!UserService.ROLE_ADMIN.equals(user.getRole()) && !java.util.Objects.equals(existing.getMerchantId(), user.getId())) {
            return Result.error("无权限修改");
        }
        attraction.setId(existing.getId());
        attraction.setMerchantId(existing.getMerchantId());
        attraction.setMerchantName(existing.getMerchantName());
        attraction.setStatus(existing.getStatus());
        attraction.setCreateTime(existing.getCreateTime());
        attraction.setUpdateTime(LocalDateTime.now());
        attractionService.updateById(attraction);
        cacheService.delete(CACHE_KEY_ATTRACTIONS);
        return Result.success(attraction);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Attraction attraction = attractionService.getById(id);
        if (attraction == null) {
            return Result.error("景点不存在");
        }
        if (!UserService.ROLE_ADMIN.equals(user.getRole()) && !attraction.getMerchantId().equals(user.getId())) {
            return Result.error("无权限删除");
        }
        attractionService.removeById(id);
        cacheService.delete(CACHE_KEY_ATTRACTIONS);
        return Result.success();
    }
    
    @GetMapping("/merchant/list")
    public Result<List<Attraction>> getMerchantAttractions(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getMerchantId, user.getId());
        return Result.success(attractionService.list(wrapper));
    }
    
    @GetMapping("/admin/pending")
    public Result<List<Attraction>> getPendingAttractions(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attraction::getStatus, 0);
        return Result.success(attractionService.list(wrapper));
    }
    
    @PutMapping("/admin/{id}/approve")
    public Result<Attraction> approveAttraction(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
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
    
    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getType, "attraction")
               .eq(Favorite::getRefId, id);
        
        List<Favorite> existing = favoriteService.list(wrapper);
        if (!existing.isEmpty()) {
            favoriteService.removeById(existing.get(0).getId());
            return Result.success("取消收藏成功");
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(user.getId());
        favorite.setStrategyId(null);
        favorite.setType("attraction");
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
               .eq(Favorite::getType, "attraction")
               .eq(Favorite::getRefId, id);
        
        return Result.success(favoriteService.count(wrapper) > 0);
    }
}
