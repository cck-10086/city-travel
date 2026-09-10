package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Favorite;
import com.citytravel.citytravel.entity.Homestay;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.FavoriteService;
import com.citytravel.citytravel.service.HomestayService;
import com.citytravel.citytravel.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/homestay")
public class HomestayController {
    
    @Autowired
    private HomestayService homestayService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @GetMapping("/list")
    public Result<List<Homestay>> list(@RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<Homestay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homestay::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Homestay::getCategoryId, categoryId);
        }
        return Result.success(homestayService.list(wrapper));
    }
    
    @GetMapping("/page")
    public Result<IPage<Homestay>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        Page<Homestay> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Homestay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homestay::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Homestay::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Homestay::getName, keyword)
                    .or().like(Homestay::getDescription, keyword)
                    .or().like(Homestay::getLocation, keyword));
        }
        IPage<Homestay> result = homestayService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/search")
    public Result<List<Homestay>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<Homestay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homestay::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Homestay::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Homestay::getName, keyword)
                    .or().like(Homestay::getDescription, keyword)
                    .or().like(Homestay::getLocation, keyword));
        }
        return Result.success(homestayService.list(wrapper));
    }
    
    @GetMapping("/{id}")
    public Result<Homestay> getById(@PathVariable Long id,
                                    @SessionAttribute(name = "user", required = false) User user) {
        Homestay homestay = homestayService.getById(id);
        if (homestay == null || (!Integer.valueOf(1).equals(homestay.getStatus())
                && (user == null || (!UserService.ROLE_ADMIN.equals(user.getRole())
                && !java.util.Objects.equals(homestay.getMerchantId(), user.getId()))))) {
            return Result.error(404, "民宿不存在");
        }
        return Result.success(homestay);
    }
    
    @PostMapping
    public Result<Homestay> save(@RequestBody Homestay homestay, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        if (!UserService.ROLE_MERCHANT.equals(user.getRole()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("商家才能发布民宿");
        }
        homestay.setMerchantId(user.getId());
        homestay.setMerchantName(user.getUsername());
        homestay.setStatus(0);
        homestay.setCreateTime(LocalDateTime.now());
        homestayService.save(homestay);
        return Result.success(homestay);
    }
    
    @PutMapping
    public Result<Homestay> update(@RequestBody Homestay homestay, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Homestay existing = homestayService.getById(homestay.getId());
        if (existing == null) {
            return Result.error("民宿不存在");
        }
        if (!UserService.ROLE_ADMIN.equals(user.getRole()) && !java.util.Objects.equals(existing.getMerchantId(), user.getId())) {
            return Result.error("无权限修改");
        }
        homestay.setId(existing.getId());
        homestay.setMerchantId(existing.getMerchantId());
        homestay.setMerchantName(existing.getMerchantName());
        homestay.setStatus(existing.getStatus());
        homestay.setCreateTime(existing.getCreateTime());
        homestay.setUpdateTime(LocalDateTime.now());
        homestayService.updateById(homestay);
        return Result.success(homestay);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Homestay homestay = homestayService.getById(id);
        if (homestay == null) {
            return Result.error("民宿不存在");
        }
        if (!UserService.ROLE_ADMIN.equals(user.getRole()) && !homestay.getMerchantId().equals(user.getId())) {
            return Result.error("无权限删除");
        }
        homestayService.removeById(id);
        return Result.success();
    }
    
    @GetMapping("/merchant/list")
    public Result<List<Homestay>> getMerchantHomestays(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<Homestay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homestay::getMerchantId, user.getId());
        return Result.success(homestayService.list(wrapper));
    }
    
    @GetMapping("/admin/pending")
    public Result<List<Homestay>> getPendingHomestays(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        LambdaQueryWrapper<Homestay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homestay::getStatus, 0);
        return Result.success(homestayService.list(wrapper));
    }
    
    @PutMapping("/admin/{id}/approve")
    public Result<Homestay> approveHomestay(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
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
    
    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id, HttpSession session) {
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
    
    @GetMapping("/{id}/favorite/check")
    public Result<Boolean> checkFavorite(@PathVariable Long id, HttpSession session) {
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
}
