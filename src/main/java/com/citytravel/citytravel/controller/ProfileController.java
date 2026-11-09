package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.util.PageUtil;
import com.citytravel.citytravel.entity.*;
import com.citytravel.citytravel.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private StrategyService strategyService;

    @Autowired
    private AttractionService attractionService;

    @Autowired
    private HomestayService homestayService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session) != null;
    }

    @GetMapping("/info")
    public Result<User> getProfile(HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        User user = getCurrentUser(session);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/info")
    public Result<User> updateProfile(@RequestBody User userData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        String oldUsername = user.getUsername();
        String newUsername = userData.getUsername();
        
        if (newUsername != null) user.setUsername(newUsername);
        if (userData.getEmail() != null) user.setEmail(userData.getEmail());
        if (userData.getPhone() != null) user.setPhone(userData.getPhone());
        
        userService.updateById(user);
        
        if (newUsername != null && !newUsername.equals(oldUsername)) {
            LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
            commentWrapper.eq(Comment::getUserId, user.getId());
            commentService.list(commentWrapper).forEach(comment -> {
                comment.setUserName(newUsername);
                commentService.updateById(comment);
            });
            
            LambdaQueryWrapper<Strategy> strategyWrapper = new LambdaQueryWrapper<>();
            strategyWrapper.eq(Strategy::getAuthorId, user.getId());
            strategyService.list(strategyWrapper).forEach(strategy -> {
                strategy.setAuthorName(newUsername);
                strategyService.updateById(strategy);
            });
        }
        
        session.setAttribute("user", user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestBody Map<String, String> passwordData,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("原密码不能为空");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 100) {
            return Result.error(400, "新密码长度必须在6至100个字符之间");
        }
        
        User sessionUser = getCurrentUser(session);
        User user = userService.getById(sessionUser.getId());
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码不正确");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        
        sessionUser.setPassword(null);
        session.setAttribute("user", sessionUser);
        
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<IPage<Order>> getMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Page<Order> pageRequest = new Page<>(PageUtil.pageNum(page), PageUtil.pageSize(size));
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, user.getId());
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        
        wrapper.orderByDesc(Order::getCreateTime);
        IPage<Order> orderPage = orderService.page(pageRequest, wrapper);
        
        return Result.success(orderPage);
    }

    @GetMapping("/orders/{id}")
    public Result<Order> getOrderById(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Order order = orderService.getById(id);
        
        if (order == null) {
            return Result.error("订单不存在");
        }
        
        if (!order.getUserId().equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        
        return Result.success(order);
    }

    @GetMapping("/favorites")
    public Result<IPage<FavoriteDetail>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Page<Favorite> pageRequest = new Page<>(PageUtil.pageNum(page), PageUtil.pageSize(size));
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId());
        wrapper.orderByDesc(Favorite::getCreateTime);
        
        IPage<Favorite> favoritePage = favoriteService.page(pageRequest, wrapper);
        
        IPage<FavoriteDetail> detailPage = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());
        detailPage.setRecords(favoritePage.getRecords().stream().map(favorite -> {
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
            } else if (favorite.getType() != null && favorite.getRefId() != null) {
                String type = favorite.getType();
                Long refId = favorite.getRefId();
                
                if ("attraction".equals(type)) {
                    Attraction attraction = attractionService.getById(refId);
                    detail.setTitle(attraction != null ? attraction.getName() : "未知景点");
                    detail.setContent(attraction != null ? attraction.getDescription() : null);
                    detail.setImage(attraction != null ? attraction.getImage() : null);
                } else if ("homestay".equals(type)) {
                    Homestay homestay = homestayService.getById(refId);
                    detail.setTitle(homestay != null ? homestay.getName() : "未知民宿");
                    detail.setContent(homestay != null ? homestay.getDescription() : null);
                    detail.setImage(homestay != null ? homestay.getImage() : null);
                } else if ("route".equals(type)) {
                    Route route = routeService.getById(refId);
                    detail.setTitle(route != null ? route.getName() : "未知线路");
                    detail.setContent(route != null ? route.getDescription() : null);
                    detail.setImage(route != null ? route.getImage() : null);
                } else {
                    detail.setTitle("未知");
                }
            }
            
            return detail;
        }).collect(java.util.stream.Collectors.toList()));
        
        return Result.success(detailPage);
    }

    @PostMapping("/favorites")
    public Result<Favorite> addFavorite(
            @RequestBody Favorite favorite,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        favorite.setUserId(user.getId());
        favorite.setCreateTime(LocalDateTime.now());
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getStrategyId, favorite.getStrategyId());
        
        if (favoriteService.count(wrapper) > 0) {
            return Result.error("已收藏");
        }
        
        favoriteService.save(favorite);
        return Result.success(favorite);
    }

    @DeleteMapping("/favorites/{id}")
    public Result<Void> removeFavorite(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Favorite favorite = favoriteService.getById(id);
        
        if (favorite == null) {
            return Result.error("收藏不存在");
        }
        
        if (!favorite.getUserId().equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        
        favoriteService.removeById(id);
        return Result.success();
    }

    @GetMapping("/comments")
    public Result<IPage<CommentDetail>> getMyComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Page<Comment> pageRequest = new Page<>(PageUtil.pageNum(page), PageUtil.pageSize(size));
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, user.getId());
        wrapper.orderByDesc(Comment::getCreateTime);
        
        IPage<Comment> commentPage = commentService.page(pageRequest, wrapper);
        
        IPage<CommentDetail> detailPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        detailPage.setRecords(commentPage.getRecords().stream().map(comment -> {
            CommentDetail detail = new CommentDetail();
            detail.setId(comment.getId());
            detail.setStrategyId(comment.getStrategyId());
            detail.setUserId(comment.getUserId());
            detail.setUserName(comment.getUserName());
            detail.setContent(comment.getContent());
            detail.setCreateTime(comment.getCreateTime());
            detail.setType(comment.getType());
            detail.setRefId(comment.getRefId());
            
            if (comment.getStrategyId() != null) {
                Strategy strategy = strategyService.getById(comment.getStrategyId());
                if (strategy != null) {
                    detail.setStrategyTitle(strategy.getTitle());
                } else {
                    detail.setStrategyTitle("未知攻略");
                }
            } else if (comment.getType() != null && comment.getRefId() != null) {
                String type = comment.getType();
                Long refId = comment.getRefId();
                
                if ("attraction".equals(type)) {
                    Attraction attraction = attractionService.getById(refId);
                    detail.setStrategyTitle(attraction != null ? attraction.getName() : "未知景点");
                } else if ("homestay".equals(type)) {
                    Homestay homestay = homestayService.getById(refId);
                    detail.setStrategyTitle(homestay != null ? homestay.getName() : "未知民宿");
                } else if ("route".equals(type)) {
                    Route route = routeService.getById(refId);
                    detail.setStrategyTitle(route != null ? route.getName() : "未知线路");
                } else {
                    detail.setStrategyTitle("未知");
                }
            } else {
                detail.setStrategyTitle("未知");
            }
            
            return detail;
        }).collect(java.util.stream.Collectors.toList()));
        
        return Result.success(detailPage);
    }

    @PostMapping("/comments")
    public Result<Comment> addComment(
            @RequestBody Comment comment,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        comment.setUserId(user.getId());
        comment.setUserName(user.getUsername());
        
        commentService.save(comment);
        return Result.success(comment);
    }

    @PutMapping("/comments/{id}")
    public Result<Comment> updateComment(
            @PathVariable Long id,
            @RequestBody Comment commentData,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Comment comment = commentService.getById(id);
        
        if (comment == null) {
            return Result.error("评论不存在");
        }
        
        if (!comment.getUserId().equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        
        if (commentData.getContent() != null) comment.setContent(commentData.getContent());
        
        commentService.updateById(comment);
        return Result.success(comment);
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Comment comment = commentService.getById(id);
        
        if (comment == null) {
            return Result.error("评论不存在");
        }
        
        if (!comment.getUserId().equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        
        commentService.removeById(id);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getProfileStats(HttpSession session) {
        if (!isLoggedIn(session)) {
            return Result.error("未登录");
        }
        
        User user = getCurrentUser(session);
        Map<String, Object> stats = new HashMap<>();
        
        long orderCount = orderService.count(new LambdaQueryWrapper<Order>().eq(Order::getUserId, user.getId()));
        long favoriteCount = favoriteService.count(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, user.getId()));
        long commentCount = commentService.count(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, user.getId()));
        
        stats.put("orderCount", orderCount);
        stats.put("favoriteCount", favoriteCount);
        stats.put("commentCount", commentCount);
        
        return Result.success(stats);
    }
}
