package com.citytravel.citytravel.config;

import com.citytravel.citytravel.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAuthenticationFilterTest {

    private final SessionAuthenticationFilter filter = new SessionAuthenticationFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldMapApprovedMerchantSessionToSecurityRole() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole("merchant");
        user.setStatus(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("user", user);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MERCHANT");
    }

    @Test
    void shouldNotGrantMerchantRoleBeforeApproval() throws Exception {
        User user = new User();
        user.setRole("merchant");
        user.setStatus(0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("user", user);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MERCHANT_PENDING");
    }
}
