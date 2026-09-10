package com.citytravel.citytravel.config;

import com.citytravel.citytravel.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void publicQueryShouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/attraction/list"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointShouldDistinguishUnauthenticatedAndForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard").session(sessionFor("tourist")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard").session(sessionFor("admin")))
                .andExpect(status().isOk());
    }

    private MockHttpSession sessionFor(String role) {
        User user = new User();
        user.setId(1L);
        user.setRole(role);
        user.setStatus(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }
}
