package com.citytravel.citytravel.service;

import com.citytravel.citytravel.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void publicRegistrationShouldAlwaysCreateTourist() {
        UserService service = spy(new UserService());
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);

        User user = new User();
        user.setUsername("attacker");
        user.setPassword("secret123");
        user.setRole("admin");
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        doReturn(true).when(service).save(user);

        service.register(user);

        assertThat(user.getRole()).isEqualTo(UserService.ROLE_TOURIST);
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getPassword()).isEqualTo("encoded");
    }
}
