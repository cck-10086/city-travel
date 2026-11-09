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
    
    /** 连续失败多少次后锁定 */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    /** 锁定时长（分钟） */
    private static final long LOCK_MINUTES = 15;

    /** 登录失败计数：key = username，value = [失败次数, 首次失败时间戳] */
    private final java.util.concurrent.ConcurrentHashMap<String, long[]> failedAttempts =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        if (isLocked(username)) {
            throw new IllegalStateException("登录失败次数过多，请 " + LOCK_MINUTES + " 分钟后再试");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = getOne(wrapper);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            failedAttempts.remove(username);
            return user;
        }
        recordFailure(username);
        return null;
    }

    /**
     * 判断账号是否处于锁定期。窗口过期会自动清零。
     */
    public boolean isLocked(String username) {
        long[] record = failedAttempts.get(username);
        if (record == null) {
            return false;
        }
        if (System.currentTimeMillis() - record[1] > LOCK_MINUTES * 60 * 1000) {
            failedAttempts.remove(username);
            return false;
        }
        return record[0] >= MAX_FAILED_ATTEMPTS;
    }

    /** 累计失败次数，同时清理过期记录，避免 Map 无限增长 */
    private void recordFailure(String username) {
        long now = System.currentTimeMillis();
        failedAttempts.entrySet().removeIf(e -> now - e.getValue()[1] > LOCK_MINUTES * 60 * 1000);
        failedAttempts.compute(username, (key, record) -> {
            if (record == null || now - record[1] > LOCK_MINUTES * 60 * 1000) {
                return new long[]{1L, now};
            }
            return new long[]{record[0] + 1, record[1]};
        });
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
