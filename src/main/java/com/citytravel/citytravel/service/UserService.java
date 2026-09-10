package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MERCHANT = "merchant";
    public static final String ROLE_TOURIST = "tourist";
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = getOne(wrapper);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }
    
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setRole(ROLE_TOURIST);
        save(user);
        return user;
    }
    
    public User registerMerchant(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(0);
        user.setRole(ROLE_MERCHANT);
        save(user);
        return user;
    }
    
    public boolean isAdmin(Long userId) {
        User user = getById(userId);
        return user != null && ROLE_ADMIN.equals(user.getRole());
    }
    
    public boolean isMerchant(Long userId) {
        User user = getById(userId);
        return user != null && ROLE_MERCHANT.equals(user.getRole()) && user.getStatus() == 1;
    }
    
    public boolean isTourist(Long userId) {
        User user = getById(userId);
        return user != null && ROLE_TOURIST.equals(user.getRole());
    }
    
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
