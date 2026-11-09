package com.citytravel.citytravel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.util.PageUtil;
import com.citytravel.citytravel.entity.Category;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.CategoryService;
import com.citytravel.citytravel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController extends BaseController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<Category>> list(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Category::getType, type);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        return Result.success(categoryService.list(wrapper));
    }

    @GetMapping("/page")
    public Result<IPage<Category>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type) {
        Page<Category> page = new Page<>(PageUtil.pageNum(pageNum), PageUtil.pageSize(pageSize));
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Category::getType, type);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        IPage<Category> result = categoryService.page(page, wrapper);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    public Result<Category> save(@RequestBody Category category, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        category.setStatus(1);
        category.setCreateTime(LocalDateTime.now());
        categoryService.save(category);
        return Result.success(category);
    }

    @PutMapping
    public Result<Category> update(@RequestBody Category category, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        Category existing = categoryService.getById(category.getId());
        if (existing == null) {
            return Result.error("分类不存在");
        }
        category.setUpdateTime(LocalDateTime.now());
        categoryService.updateById(category);
        return Result.success(category);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !UserService.ROLE_ADMIN.equals(user.getRole())) {
            return Result.error("无权限");
        }
        categoryService.removeById(id);
        return Result.success();
    }

    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        List<String> types = Arrays.asList("attraction", "homestay", "route");
        return Result.success(types);
    }
}