package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Comment;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.CommentService;
import com.citytravel.citytravel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public Result<Comment> save(@Valid @RequestBody Comment comment, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        comment.setUserId(user.getId());
        comment.setUserName(user.getUsername());
        comment.setCreateTime(LocalDateTime.now());
        
        if (comment.getReplyToId() != null) {
            Comment targetComment = commentService.getById(comment.getReplyToId());
            if (targetComment != null) {
                comment.setReplyToName(targetComment.getUserName());
                if (targetComment.getParentId() != null) {
                    comment.setParentId(targetComment.getParentId());
                } else {
                    comment.setParentId(targetComment.getId());
                }
            }
        }
        
        commentService.save(comment);
        return Result.success(comment);
    }
    
    @GetMapping("/item/{type}/{id}")
    public Result<List<Comment>> getByItem(@PathVariable String type, @PathVariable Long id) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if ("strategy".equals(type)) {
            wrapper.eq(Comment::getStrategyId, id);
        } else {
            wrapper.eq(Comment::getRefId, id)
                   .eq(Comment::getType, type);
        }
        wrapper.orderByDesc(Comment::getCreateTime);
        List<Comment> comments = commentService.list(wrapper);
        return Result.success(buildCommentTree(comments));
    }
    
    @GetMapping("/tree/item/{type}/{id}")
    public Result<List<Comment>> getCommentTree(@PathVariable String type, @PathVariable Long id) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if ("strategy".equals(type)) {
            wrapper.eq(Comment::getStrategyId, id);
        } else {
            wrapper.eq(Comment::getRefId, id)
                   .eq(Comment::getType, type);
        }
        wrapper.orderByDesc(Comment::getCreateTime);
        List<Comment> comments = commentService.list(wrapper);
        return Result.success(buildCommentTree(comments));
    }
    
    private List<Comment> buildCommentTree(List<Comment> comments) {
        List<Comment> tree = new ArrayList<>();
        Map<Long, Comment> commentMap = new HashMap<>();
        
        for (Comment comment : comments) {
            comment.setChildren(new ArrayList<>());
            commentMap.put(comment.getId(), comment);
        }
        
        for (Comment comment : comments) {
            if (comment.getParentId() != null) {
                Comment parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getChildren().add(comment);
                }
            } else {
                tree.add(comment);
            }
        }
        
        for (Comment comment : tree) {
            comment.getChildren().sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        }
        
        return tree;
    }
    
    @PostMapping("/reply/{commentId}")
    public Result<Comment> replyComment(@PathVariable Long commentId, 
                                        @Valid @RequestBody Comment replyComment, 
                                        HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        
        Comment parentComment = commentService.getById(commentId);
        if (parentComment == null) {
            return Result.error("评论不存在");
        }
        
        replyComment.setUserId(user.getId());
        replyComment.setUserName(user.getUsername());
        replyComment.setCreateTime(LocalDateTime.now());
        replyComment.setReplyToId(commentId);
        replyComment.setReplyToName(parentComment.getUserName());
        
        if (parentComment.getParentId() != null) {
            replyComment.setParentId(parentComment.getParentId());
        } else {
            replyComment.setParentId(commentId);
        }
        
        if ("strategy".equals(parentComment.getType()) || parentComment.getStrategyId() != null) {
            replyComment.setStrategyId(parentComment.getStrategyId());
            replyComment.setType(parentComment.getType());
            replyComment.setRefId(parentComment.getRefId());
        } else {
            replyComment.setStrategyId(parentComment.getStrategyId());
            replyComment.setType(parentComment.getType());
            replyComment.setRefId(parentComment.getRefId());
        }
        
        commentService.save(replyComment);
        return Result.success(replyComment);
    }
    
    @PostMapping("/item/{type}/{id}")
    public Result<Comment> addItemComment(@PathVariable String type, @PathVariable Long id, 
                                          @Valid @RequestBody Comment comment, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        comment.setUserId(user.getId());
        comment.setUserName(user.getUsername());
        comment.setCreateTime(LocalDateTime.now());
        
        if (comment.getReplyToId() != null) {
            Comment targetComment = commentService.getById(comment.getReplyToId());
            if (targetComment != null) {
                comment.setReplyToName(targetComment.getUserName());
                if (targetComment.getParentId() != null) {
                    comment.setParentId(targetComment.getParentId());
                } else {
                    comment.setParentId(targetComment.getId());
                }
            }
        }
        
        if ("strategy".equals(type)) {
            comment.setStrategyId(id);
            comment.setType(null);
            comment.setRefId(null);
        } else {
            comment.setStrategyId(null);
            comment.setRefId(id);
            comment.setType(type);
        }
        commentService.save(comment);
        return Result.success(comment);
    }
    
    @GetMapping("/user/{userId}")
    public Result<List<Comment>> getByUser(@PathVariable Long userId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getUserId, userId)
               .orderByDesc(Comment::getCreateTime);
        return Result.success(commentService.list(wrapper));
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error("请先登录");
        }
        Comment comment = commentService.getById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        if (!user.getId().equals(comment.getUserId()) && !"admin".equals(user.getRole())) {
            return Result.error("无权限删除");
        }
        
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, id);
        commentService.remove(wrapper);
        
        commentService.removeById(id);
        return Result.success();
    }
}
