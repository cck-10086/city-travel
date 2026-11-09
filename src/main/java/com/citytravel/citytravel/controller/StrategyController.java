package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.util.PageUtil;
import com.citytravel.citytravel.entity.Comment;
import com.citytravel.citytravel.entity.Favorite;
import com.citytravel.citytravel.entity.Strategy;
import com.citytravel.citytravel.service.CommentService;
import com.citytravel.citytravel.service.FavoriteService;
import com.citytravel.citytravel.service.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.UserService;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController extends BaseController {
    
    @Autowired
    private StrategyService strategyService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/list")
    public Result<List<Strategy>> list() {
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getStatus, 1);
        wrapper.orderByDesc(Strategy::getCreateTime);
        return Result.success(strategyService.list(wrapper));
    }
    
    @GetMapping("/page")
    public Result<IPage<Strategy>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<Strategy> page = new Page<>(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getStatus, 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Strategy::getTitle, keyword)
                    .or().like(Strategy::getContent, keyword));
        }
        wrapper.orderByDesc(Strategy::getViews);
        IPage<Strategy> result = strategyService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/search")
    public Result<List<Strategy>> search(@RequestParam String keyword) {
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getStatus, 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(Strategy::getTitle, keyword)
                    .or().like(Strategy::getContent, keyword));
        }
        wrapper.orderByDesc(Strategy::getViews);
        return Result.success(strategyService.list(wrapper));
    }
    
    @GetMapping("/{id}")
    public Result<Strategy> getById(@PathVariable Long id,
                                    @SessionAttribute(name = "user", required = false) User user) {
        Strategy strategy = strategyService.getById(id);
        if (strategy == null || (!Integer.valueOf(1).equals(strategy.getStatus())
                && (user == null || (!UserService.ROLE_ADMIN.equals(user.getRole())
                && !java.util.Objects.equals(strategy.getAuthorId(), user.getId()))))) {
            return Result.error(404, "攻略不存在");
        }
        strategyService.incrementViews(id);
        return Result.success(strategy);
    }
    
    @PostMapping
    public Result<Strategy> save(@Valid @RequestBody Strategy strategy, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return Result.error("请先登录");
        }
        strategy.setAuthorId(user.getId());
        strategy.setAuthorName(user.getUsername());
        strategy.setViews(0);
        strategy.setLikes(0);
        strategy.setStatus(0);  // 设置为待审核状态
        strategy.setCreateTime(LocalDateTime.now());
        strategyService.save(strategy);
        return Result.success(strategy);
    }
    
    @PostMapping("/like/{id}")
    public Result<Void> like(@PathVariable Long id) {
        strategyService.incrementLikes(id);
        return Result.success();
    }
    
    @GetMapping("/{id}/comments")
    public Result<List<Comment>> getComments(@PathVariable Long id) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getStrategyId, id);
        wrapper.orderByDesc(Comment::getCreateTime);
        return Result.success(commentService.list(wrapper));
    }
    
    @PostMapping("/{id}/comments")
    public Result<Comment> addComment(@PathVariable Long id, @RequestBody Comment comment, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        comment.setStrategyId(id);
        comment.setUserId(((User) user).getId());
        comment.setUserName(((User) user).getUsername());
        comment.setCreateTime(LocalDateTime.now());
        commentService.save(comment);
        return Result.success(comment);
    }
    
    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return Result.error("请先登录");
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getStrategyId, id);
        
        List<Favorite> existing = favoriteService.list(wrapper);
        if (!existing.isEmpty()) {
            favoriteService.removeById(existing.get(0).getId());
            return Result.success("取消收藏成功");
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(user.getId());
        favorite.setStrategyId(id);
        favorite.setType("strategy");
        favorite.setCreateTime(LocalDateTime.now());
        favoriteService.save(favorite);
        return Result.success("收藏成功");
    }
    
    @GetMapping("/{id}/favorite/check")
    public Result<Boolean> checkFavorite(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return Result.success(false);
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getStrategyId, id);
        
        return Result.success(favoriteService.count(wrapper) > 0);
    }
    
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return Result.error("请先登录");
        }
        
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, user.getId())
               .eq(Favorite::getStrategyId, id);
        
        favoriteService.remove(wrapper);
        return Result.success();
    }
    
    @GetMapping("/user/{userId}")
    public Result<List<Strategy>> getByUserId(
            @PathVariable Long userId,
            @SessionAttribute(name = "user", required = false) User user) {
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getAuthorId, userId);
        if (user == null || (!userId.equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole()))) {
            wrapper.eq(Strategy::getStatus, 1);
        }
        wrapper.orderByDesc(Strategy::getCreateTime);
        return Result.success(strategyService.list(wrapper));
    }
    
    @PutMapping("/{id}")
    public Result<Strategy> update(@PathVariable Long id, @Valid @RequestBody Strategy strategy, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        Strategy existing = strategyService.getById(id);
        if (existing == null) {
            return Result.error("攻略不存在");
        }
        
        User currentUser = (User) user;
        if (!java.util.Objects.equals(existing.getAuthorId(), currentUser.getId()) && !"admin".equals(currentUser.getRole())) {
            return Result.error("只能编辑自己的攻略");
        }
        
        strategy.setId(id);
        strategy.setAuthorId(existing.getAuthorId());
        strategy.setAuthorName(existing.getAuthorName());
        strategy.setViews(existing.getViews());
        strategy.setLikes(existing.getLikes());
        strategy.setStatus(UserService.ROLE_ADMIN.equals(currentUser.getRole()) ? existing.getStatus() : 0);
        strategy.setCreateTime(existing.getCreateTime());
        strategy.setUpdateTime(LocalDateTime.now());
        strategyService.updateById(strategy);
        return Result.success(strategyService.getById(id));
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return Result.error("请先登录");
        }
        
        Strategy existing = strategyService.getById(id);
        if (existing == null) {
            return Result.error("攻略不存在");
        }
        
        if (!existing.getAuthorId().equals(user.getId()) && !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("只能删除自己的攻略");
        }
        
        strategyService.removeById(id);
        return Result.success();
    }
    
    @GetMapping("/admin/pending")
    public Result<List<Strategy>> getPendingStrategies(HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        LambdaQueryWrapper<Strategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Strategy::getStatus, 0);
        wrapper.orderByDesc(Strategy::getCreateTime);
        return Result.success(strategyService.list(wrapper));
    }
    
    @PutMapping("/admin/{id}/approve")
    public Result<Strategy> approveStrategy(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
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
    
    @PutMapping("/admin/{id}/reject")
    public Result<Strategy> rejectStrategy(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        Strategy strategy = strategyService.getById(id);
        if (strategy != null) {
            strategy.setStatus(-1);
            strategyService.updateById(strategy);
            return Result.success(strategy);
        }
        return Result.error("攻略不存在");
    }
}
