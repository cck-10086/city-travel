package com.citytravel.citytravel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .addFilterBefore(new SessionAuthenticationFilter(), AnonymousAuthenticationFilter.class)
            .authorizeRequests()
                .antMatchers("/", "/index.html", "/favicon.ico", "/error", "/images/**").permitAll()
                .antMatchers("/api/user/login", "/api/user/register", "/api/user/register/merchant").permitAll()
                .antMatchers("/api/user/logout", "/api/user/current").permitAll()
                .antMatchers("/api/admin/**", "/api/statistics/**", "/api/trip-plan/ai/cost").hasRole("ADMIN")
                .antMatchers("/api/*/admin/**").hasRole("ADMIN")
                .antMatchers("/api/*/merchant/**").hasAnyRole("MERCHANT", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/attraction", "/api/homestay", "/api/route")
                    .hasAnyRole("MERCHANT", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/attraction", "/api/homestay", "/api/route")
                    .hasAnyRole("MERCHANT", "ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/attraction/*", "/api/homestay/*", "/api/route/*")
                    .hasAnyRole("MERCHANT", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/category/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/category/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/category/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET,
                        "/api/attraction/list", "/api/attraction/page", "/api/attraction/search", "/api/attraction/*",
                        "/api/homestay/list", "/api/homestay/page", "/api/homestay/search", "/api/homestay/*",
                        "/api/route/list", "/api/route/page", "/api/route/search", "/api/route/*",
                        "/api/strategy/list", "/api/strategy/page", "/api/strategy/search", "/api/strategy/*",
                        "/api/strategy/*/comments", "/api/strategy/user/*",
                        "/api/category/**", "/api/comment/**", "/api/evaluation/order/*",
                        "/api/trip-plan/ai/status").permitAll()
                .antMatchers(HttpMethod.POST, "/api/trip-plan/generate").permitAll()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling(exceptionHandling -> exceptionHandling
                    .authenticationEntryPoint((request, response, exception) ->
                            writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录"))
                    .accessDeniedHandler((request, response, exception) ->
                            writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN, "无权限执行此操作")))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        
        return http.build();
    }

    private static void writeSecurityError(HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
