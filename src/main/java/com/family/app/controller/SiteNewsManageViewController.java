package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang quản lý tin toàn site (MANAGE_SITE_NEWS).
 * {@code GET /family-head} do {@link PageController} phục vụ (dashboard trưởng họ).
 */
@Controller
public class SiteNewsManageViewController {

    @GetMapping("/site-news-manage")
    public String siteNewsManagePage(Model model) {
        model.addAttribute("activeMenu", "news");
        return "public/site-news-manage";
    }
}
