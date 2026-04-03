package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * URL cũ — chuyển hết vào cổng quản trị dòng họ (tin công khai theo từng family).
 */
@Controller
public class SiteNewsManageViewController {

    @GetMapping("/site-news-manage")
    public RedirectView siteNewsManage() {
        return new RedirectView("/family-head?tab=news", true);
    }
}
