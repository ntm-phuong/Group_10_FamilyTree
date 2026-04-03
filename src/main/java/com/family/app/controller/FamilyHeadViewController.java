package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang Quản trị dòng họ (FE): tin tức / sự kiện — gọi API {@code /api/family-head/news}.
 */
@Controller
public class FamilyHeadViewController {

    @GetMapping("/family-head")
    public String familyHeadNews(Model model) {
        model.addAttribute("activeMenu", "family-head");
        return "public/family-head";
    }
}
