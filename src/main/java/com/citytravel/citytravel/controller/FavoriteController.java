package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Favorite;
import com.citytravel.citytravel.entity.FavoriteDetail;
import com.citytravel.citytravel.entity.Strategy;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.FavoriteService;
import com.citytravel.citytravel.service.StrategyService;
import com.citytravel.citytravel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private StrategyService strategyService;
    
    @PostMapping("/attraction/{id}/favorite")
    public Result<?> toggleAttractionFavorite(@PathVariable Long id, HttpSession session) {
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
    
    @GetMapping("/attraction/{id}/favorite/check")
    public Result<Boolean> checkAttractionFavorite(@PathVariable Long id, HttpSession session) {
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
    
    @PostMapping("/homestay/{id}/favorite")
    public Result<?> toggleHomestayFavorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getType, "homestay")
               .eq(Favorite::getRefId, id);
        
        List<Favorite> existing = favoriteService.list(wrapper);
        if (!existing.isEmpty()) {
            favoriteService.removeById(existing.get(0).getId());
            return Result.success("取消收藏成功");
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(user.getId());
        favorite.setStrategyId(null);
        favorite.setType("homestay");
        favorite.setRefId(id);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteService.save(favorite);
        return Result.success("收藏成功");
    }
    
    @GetMapping("/homestay/{id}/favorite/check")
    public Result<Boolean> checkHomestayFavorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.success(false);
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getType, "homestay")
               .eq(Favorite::getRefId, id);
        
        return Result.success(favoriteService.count(wrapper) > 0);
    }
    
    @PostMapping("/route/{id}/favorite")
    public Result<?> toggleRouteFavorite(@PathVariable Long id, HttpSession session) {
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
    
    @GetMapping("/route/{id}/favorite/check")
    public Result<Boolean> checkRouteFavorite(@PathVariable Long id, HttpSession session) {
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
    
    @GetMapping("/user/{userId}/favorites")
    public Result<List<FavoriteDetail>> getUserFavorites(@PathVariable Long userId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        if (!userId.equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error(403, "无权限查看其他用户的收藏");
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .orderByDesc(Favorite::getCreateTime);
        
        List<Favorite> favorites = favoriteService.list(wrapper);
        List<FavoriteDetail> details = favorites.stream().map(favorite -> {
            FavoriteDetail detail = new FavoriteDetail();
            detail.setId(favorite.getId());
            detail.setUserId(favorite.getUserId());
            detail.setStrategyId(favorite.getStrategyId());
            detail.setType(favorite.getType());
            detail.setRefId(favorite.getRefId());
            detail.setCreateTime(favorite.getCreateTime());
            
            if (favorite.getStrategyId() != null) {
                Strategy strategy = strategyService.getById(favorite.getStrategyId());
                if (strategy != null) {
                    detail.setTitle(strategy.getTitle());
                    detail.setContent(strategy.getContent());
                    detail.setAuthorName(strategy.getAuthorName());
                    detail.setImage(strategy.getImage());
                }
            }
            
            return detail;
        }).collect(java.util.stream.Collectors.toList());
        
        return Result.success(details);
    }
    
    @DeleteMapping("/favorite/{id}")
    public Result<Void> deleteFavorite(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Favorite favorite = favoriteService.getById(id);
        if (favorite == null) {
            return Result.error("收藏不存在");
        }
        if (!user.getId().equals(favorite.getUserId()) && !"admin".equals(user.getRole())) {
            return Result.error("无权限删除");
        }
        favoriteService.removeById(id);
        return Result.success();
    }
}
