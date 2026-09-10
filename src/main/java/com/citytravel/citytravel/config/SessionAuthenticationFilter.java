package com.citytravel.citytravel.config;

import com.citytravel.citytravel.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

/**
 * 将项目原有的 Session 用户转换为 Spring Security 身份。
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            Object principal = session == null ? null : session.getAttribute("user");
            if (principal instanceof User) {
                User user = (User) principal;
                String role = user.getRole() == null ? "tourist" : user.getRole();
                if ("merchant".equals(role) && !Integer.valueOf(1).equals(user.getStatus())) {
                    role = "merchant_pending";
                }
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                        "ROLE_" + role.toUpperCase(Locale.ROOT));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.singleton(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
