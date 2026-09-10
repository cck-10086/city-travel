package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.UserService;

import javax.servlet.http.HttpSession;

public class BaseController {

    protected User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }

    protected boolean isAdmin(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null && UserService.ROLE_ADMIN.equals(user.getRole());
    }

    protected boolean isMerchant(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null && UserService.ROLE_MERCHANT.equals(user.getRole()) && user.getStatus() == 1;
    }

    protected boolean isTourist(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null && UserService.ROLE_TOURIST.equals(user.getRole());
    }

    protected void clearPassword(User user) {
        if (user != null) {
            user.setPassword(null);
        }
    }

    protected User getCurrentUserWithClearPassword(HttpSession session) {
        User user = getCurrentUser(session);
        clearPassword(user);
        return user;
    }
}