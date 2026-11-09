package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.citytravel.citytravel.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@DisplayName("登录失败限流")
class UserServiceLockTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        User stored = new User();
        stored.setId(1L);
        stored.setUsername("alice");
        stored.setPassword(encoder.encode("correct-password"));

        // 用 spy 直接桩掉 getOne，绕开 MyBatis-Plus 内部的 mapper 调用细节，
        // 让测试只关注限流逻辑本身。
        userService = spy(new UserService());
        ReflectionTestUtils.setField(userService, "passwordEncoder", encoder);
        doReturn(stored).when(userService).getOne(any(Wrapper.class));
    }

    @Test
    @DisplayName("密码正确时登录成功且清空失败计数")
    void shouldLoginWithCorrectPassword() {
        User user = userService.login("alice", "correct-password");

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(userService.isLocked("alice")).isFalse();
    }

    @Test
    @DisplayName("连续失败 5 次后账号锁定，锁定期内正确密码也不放行")
    void shouldLockAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            assertThat(userService.login("alice", "wrong")).isNull();
        }

        assertThat(userService.isLocked("alice")).isTrue();
        assertThatThrownBy(() -> userService.login("alice", "correct-password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("登录失败次数过多");
    }

    @Test
    @DisplayName("不同账号的失败计数互不影响")
    void shouldIsolateCountersPerAccount() {
        for (int i = 0; i < 5; i++) {
            userService.login("alice", "wrong");
        }

        assertThat(userService.isLocked("alice")).isTrue();
        assertThat(userService.isLocked("bob")).isFalse();
    }
}
