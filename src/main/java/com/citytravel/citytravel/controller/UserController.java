package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public Result<User> login(@Valid @RequestBody User user, HttpServletRequest request) {
        User loginUser = userService.login(user.getUsername(), user.getPassword());
        if (loginUser != null) {
            HttpSession session = request.getSession();
            request.changeSessionId();
            session.setAttribute("user", loginUser);
            loginUser.setPassword(null);
            return Result.success(loginUser);
        }
        return Result.error("用户名或密码错误");
    }
    
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        User registeredUser = userService.register(user);
        registeredUser.setPassword(null);
        return Result.success(registeredUser);
    }
    
    @PostMapping("/register/merchant")
    public Result<User> registerMerchant(@Valid @RequestBody User user) {
        User registeredUser = userService.registerMerchant(user);
        registeredUser.setPassword(null);
        return Result.success(registeredUser);
    }
    
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }
    
    @GetMapping("/current")
    public Result<User> getCurrentUser(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            User user = userService.getById(sessionUser.getId());
            if (user != null) {
                user.setPassword(null);
                session.setAttribute("user", user);
                return Result.success(user);
            }
        }
        return Result.error("未登录");
    }
}
