package com.citytravel.citytravel.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 仅用于本地演示，默认关闭，避免生产环境出现固定测试账号。
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;

    @Value("${app.demo-data.password:}")
    private String demoPassword;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (demoPassword == null || demoPassword.length() < 8) {
            throw new IllegalStateException("启用演示数据时必须配置至少8位的 APP_DEMO_DATA_PASSWORD");
        }

        createUserIfAbsent("admin", "admin@example.com", UserService.ROLE_ADMIN, 1);
        createUserIfAbsent("test", "test@example.com", UserService.ROLE_TOURIST, 1);
        createUserIfAbsent("merchant", "merchant@example.com", UserService.ROLE_MERCHANT, 1);
        logger.info("本地演示账号初始化完成");
    }

    private void createUserIfAbsent(String username, String email, String role, int status) {
        long count = userService.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(userService.encodePassword(demoPassword));
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        userService.save(user);
    }
}
