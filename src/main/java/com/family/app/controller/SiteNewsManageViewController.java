package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Trang quản lý tin toàn site (MANAGE_SITE_NEWS) và chuyển hướng URL cũ /family-head.
 */
@Controller
public class SiteNewsManageViewController {

    @GetMapping("/family-head")
    public RedirectView familyHeadLegacyRedirect() {
        return new RedirectView("/news", true);
    }

    @GetMapping("/site-news-manage")
    public String siteNewsManagePage(Model model) {
        model.addAttribute("activeMenu", "news");
        return "public/site-news-manage";
    }
}
